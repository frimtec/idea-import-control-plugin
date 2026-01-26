package com.github.frimtec.idea.plugin.importcontrol;

import com.github.frimtec.idea.plugin.importcontrol.OptionDialogHelper.Option;
import com.github.frimtec.libraries.importcontrol.api.ExportPackage;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


public class ImportControlInspection extends LocalInspectionTool {

    private final static Logger LOGGER = Logger.getInstance(ImportControlInspection.class);

    @SuppressWarnings({"WeakerAccess", "PublicField"})
    @NonNls
    public String exportAnnotation = ExportPackage.class.getName();

    @SuppressWarnings({"WeakerAccess", "PublicField"})
    @NonNls
    public String rootPackages = "";

    private InspectionOptions inspectionOptions = InspectionOptions.of(this.exportAnnotation, this.rootPackages);


    private final List<Option> options = Arrays.asList(
            Option.create("Package export annotation", () -> this.exportAnnotation, (value) -> {
                this.exportAnnotation = value;
                this.inspectionOptions = InspectionOptions.of(this.exportAnnotation, this.rootPackages);
            }),
            Option.create("Root packages (separate with ;)", () -> this.rootPackages, (value) -> {
                this.rootPackages = value;
                this.inspectionOptions = InspectionOptions.of(this.exportAnnotation, this.rootPackages);
            })
    );

    @Override
    public void readSettings(@NotNull Element node) {
        super.readSettings(node);
        this.inspectionOptions = InspectionOptions.of(this.exportAnnotation, this.rootPackages);
    }

    @Override
    public void writeSettings(@NotNull Element node) {
        this.exportAnnotation = this.inspectionOptions.getExportAnnotation();
        this.rootPackages = String.join(";", this.inspectionOptions.getRootPackages());
        super.writeSettings(node);
    }

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
                super.visitReferenceElement(reference);
                analyseReference(reference);
            }

            @Override
            public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
                super.visitReferenceExpression(expression);
                analyseReference(expression);
            }

            private void analyseReference(PsiJavaCodeReferenceElement reference) {
                PsiElement psiElement = reference.resolve();
                if (psiElement instanceof PsiClass psiClass && !isTypeIgnored(psiElement)) {
                    Optional<String> optionalPackageName = findPackageName(psiClass);
                    optionalPackageName.ifPresentOrElse(
                            packageName -> {
                                if (isMonitored(inspectionOptions, packageName) && isOtherModule(reference, psiClass)) {
                                    PsiPackage foundPackage = JavaPsiFacade.getInstance(holder.getProject()).findPackage(packageName);
                                    if (foundPackage != null) {
                                        if (!hasAnnotationInPackage(foundPackage, inspectionOptions.getExportAnnotation())) {
                                            Module moduleForFile = ModuleUtilCore.findModuleForFile(psiClass.getContainingFile());
                                            holder.registerProblem(reference, String.format(
                                                            "'%s' is module private and not allowed to use outside its module '%s'",
                                                            psiClass.getName(),
                                                            moduleForFile != null ? moduleForFile.getName() : "<unknown>"
                                                    )
                                            );
                                        }
                                    } else {
                                        LOGGER.warn(
                                                String.format(
                                                        "PsiClass element with missing package => reference ignored; PsiClass type: %s, PsiClass name: %s, Text: %s",
                                                        psiClass.getClass(),
                                                        psiClass.getName(),
                                                        psiClass.getText()
                                                )
                                        );
                                    }
                                }
                            },
                            () -> LOGGER.warn(
                                    String.format(
                                            "PsiClass element with no package => reference ignored; PsiClass type: %s, PsiClass name: %s, Text: %s",
                                            psiClass.getClass(),
                                            psiClass.getName(),
                                            psiClass.getText()
                                    )
                            ));
                }
            }

            private boolean hasAnnotationInPackage(PsiPackage psiPackage, String annotationToCheckFor) {
                var found = new AtomicBoolean(false);
                PsiFile[] files = psiPackage.getFiles(GlobalSearchScope.allScope(psiPackage.getProject()));
                for (PsiFile file : files) {
                    JavaPsiAnnotationUtil.processPackageAnnotations(file, (annotation, superPackage) -> {
                        if (annotationToCheckFor.equals(annotation.getQualifiedName())) {
                            found.set(true);
                        }
                    }, false);
                    if (found.get()) {
                        break;
                    }
                }
                return found.get();
            }

            private boolean isTypeIgnored(PsiElement psiElement) {
                return psiElement instanceof PsiTypeParameter;
            }

            private boolean isMonitored(InspectionOptions options, String packageName) {
                for (String root : options.getRootPackages()) {
                    if (packageName.startsWith(root)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean isOtherModule(PsiJavaCodeReferenceElement reference, PsiClass psiClass) {
                Module moduleCaller = ModuleUtilCore.findModuleForFile(reference.getContainingFile());
                Module moduleCallee = ModuleUtilCore.findModuleForFile(psiClass.getContainingFile());
                if (moduleCaller == null) {
                    LOGGER.warn(
                            String.format(
                                    "Caller reference not in module => reference ignored; Name: %s, Text: %s",
                                    reference.getQualifiedName(),
                                    reference.getText()
                            )
                    );
                    return false;
                }
                return !moduleCaller.equals(moduleCallee);
            }

            private Optional<String> findPackageName(PsiClass psiClass) {
                String qualifiedName = unwrapInnerClass(psiClass).getQualifiedName();
                return qualifiedName != null ? Optional.of(stripClassName(qualifiedName)) : Optional.empty();
            }

            private String stripClassName(String qualifiedName) {
                int lastSeparatorIndex = qualifiedName.lastIndexOf(".");
                return lastSeparatorIndex != -1 ? qualifiedName.substring(0, lastSeparatorIndex) : qualifiedName;
            }

            private PsiClass unwrapInnerClass(PsiClass psiClass) {
                return psiClass.getContainingClass() != null ? unwrapInnerClass(psiClass.getContainingClass()) : psiClass;
            }
        };
    }

    @Override
    public JComponent createOptionsPanel() {
        return OptionDialogHelper.createOptionsPanel(this.options);
    }
}

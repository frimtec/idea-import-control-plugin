package com.github.frimtec.idea.plugin.importcontrol;

import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods related to {@link PsiAnnotation}.
 * TODO: Migrate to com.intellij.java.codeserver.core.JavaPsiAnnotationUtil as of platform 2026.1
 */
final class JavaPsiAnnotationUtil {
    private JavaPsiAnnotationUtil() {
    }

    /**
     * Process package annotations related to the specified {@link PsiFile}.
     * Only annotations declared in package-info files within the same source root will be processed.
     * Both source and class files are supported.
     *
     * @param file                 context {@link PsiFile} to find and process annotations related to
     * @param processor            a processor function
     * @param processSuperPackages whether to process annotations from super-packages.
     *                             A super-package is a package whose qualified name is a prefix of a current package name
     *                             (e.g., package "com.example" is a super-package of "com.example.foo")
     */
    public static void processPackageAnnotations(@NotNull PsiFile file,
                                                 @NotNull PackageAnnotationProcessor processor,
                                                 boolean processSuperPackages) {
        boolean superPackage = false;
        ProjectFileIndex index = ProjectRootManager.getInstance(file.getProject()).getFileIndex();
        VirtualFile vFile = file.getVirtualFile();
        if (vFile == null) return;
        VirtualFile root = index.getSourceRootForFile(vFile);
        boolean compiled = false;
        if (root == null) {
            root = index.getClassRootForFile(vFile);
            if (root == null) return;
            compiled = true;
        }
        // Single-file source root -- no package-info processing for now
        if (root.equals(vFile)) return;
        PsiDirectory directory = file.getContainingDirectory();
        while (directory != null) {
            PsiFile packageFile = directory.findFile(compiled ? PsiPackage.PACKAGE_INFO_CLS_FILE : PsiPackage.PACKAGE_INFO_FILE);
            if (packageFile instanceof PsiJavaFile javaFile) {
                PsiPackageStatement stmt = javaFile.getPackageStatement();
                if (stmt != null) {
                    PsiModifierList modifierList = stmt.getAnnotationList();
                    if (modifierList != null) {
                        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                            processor.process(annotation, superPackage);
                        }
                    }
                }
            }
            if (!processSuperPackages || root.equals(directory.getVirtualFile())) break;
            directory = directory.getParentDirectory();
            superPackage = true;
        }
    }

    /**
     * A functional interface to be used with {@link #processPackageAnnotations(PsiFile, PackageAnnotationProcessor, boolean)}
     */
    @FunctionalInterface
    public interface PackageAnnotationProcessor {
        /**
         * @param annotation   applicable package annotation
         * @param superPackage whether the annotation is from a super-package
         */
        void process(@NotNull PsiAnnotation annotation, boolean superPackage);
    }
}
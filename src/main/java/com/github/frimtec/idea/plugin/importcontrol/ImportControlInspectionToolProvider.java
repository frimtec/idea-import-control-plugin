package com.github.frimtec.idea.plugin.importcontrol;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import org.jetbrains.annotations.NotNull;

public class ImportControlInspectionToolProvider implements InspectionToolProvider {
    @Override
    public @NotNull Class<? extends LocalInspectionTool>[] getInspectionClasses() {
        //noinspection unchecked
        return new Class[]{ImportControlInspection.class};
    }
}

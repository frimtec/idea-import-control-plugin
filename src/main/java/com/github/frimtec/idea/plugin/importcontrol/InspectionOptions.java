package com.github.frimtec.idea.plugin.importcontrol;

@SuppressWarnings({"AssignmentOrReturnOfFieldWithMutableType"})
final class InspectionOptions {
  private final String exportAnnotation;
  private final String[] rootPackages;

  private InspectionOptions(String exportAnnotation, String rootPackages) {
    this.exportAnnotation = exportAnnotation;
    this.rootPackages = splitOptions(rootPackages);
  }

  static InspectionOptions of(String exportAnnotation, String rootPackages) {
    return new InspectionOptions(exportAnnotation, rootPackages);
  }

  private static String[] splitOptions(String optionValue) {
    return optionValue != null ? optionValue.split(";") : new String[0];
  }

    public String getExportAnnotation() {
        return exportAnnotation;
    }

    public String[] getRootPackages() {
    return this.rootPackages;
  }
}

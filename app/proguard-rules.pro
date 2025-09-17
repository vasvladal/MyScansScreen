# Add project specific ProGuard rules here.
# General POI compatibility rules for Android
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.xmlgraphics.**
-dontwarn org.apache.batik.**
-dontwarn org.openxmlformats.schemas.**

# Keep essential POI classes
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class schemasMicrosoftComOffice.** { *; }
# Keep annotation classes
-keep class javax.annotation.** { *; }
-keep class org.jetbrains.annotations.** { *; }
-keep class com.google.auto.value.** { *; }
-keep class com.google.code.findbugs.annotations.** { *; }

# Suppress warnings for annotation conflicts
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn com.google.auto.value.**
-dontwarn com.google.code.findbugs.annotations.**

# AppCrawler specific rules
-keep class com.google.android.appcrawler.** { *; }
-dontwarn com.google.android.appcrawler.**

# Markwon specific rules
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**
-keep class io.noties.prism4j.** { *; }
-dontwarn io.noties.prism4j.**

# OpenCV rules
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Krop library rules
-keep class com.attafitamim.krop.** { *; }
-dontwarn com.attafitamim.krop.**

# General rules for dependency conflicts
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.lang.model.**
-dontwarn com.sun.tools.javac.**

# Keep BuildConfig
-keep class **.BuildConfig { *; }

# Keep version information
-keep class * {
    public static final java.lang.String VERSION_NAME;
    public static final int VERSION_CODE;
}

# ===============================================
# MISSING RULES - Generated automatically by R8
# ===============================================

# JavaParser warnings
-dontwarn com.github.javaparser.ParserConfiguration
-dontwarn com.github.javaparser.symbolsolver.model.resolution.TypeSolver
-dontwarn com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
-dontwarn com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
-dontwarn com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
-dontwarn com.github.javaparser.utils.CollectionStrategy

# Compression libraries
-dontwarn com.github.luben.zstd.ZstdInputStream
-dontwarn com.github.luben.zstd.ZstdOutputStream
-dontwarn org.brotli.dec.BrotliInputStream
-dontwarn org.tukaani.xz.**

# PDF and graphics libraries
-dontwarn de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextDrawer
-dontwarn org.apache.pdfbox.pdmodel.PDDocument
-dontwarn org.apache.batik.**

# AWT/Swing (desktop graphics - not available on Android)
-dontwarn java.awt.**
-dontwarn java.awt.image.**
-dontwarn java.awt.geom.**
-dontwarn java.awt.font.FontRenderContext
-dontwarn java.awt.color.ColorSpace

# XML processing
-dontwarn javax.xml.crypto.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.xml.security.Init

# Saxon XML processor
-dontwarn net.sf.saxon.**

# Build tools and frameworks
-dontwarn org.apache.maven.plugin.AbstractMojo
-dontwarn org.apache.maven.plugins.annotations.Mojo
-dontwarn org.apache.tools.ant.taskdefs.MatchingTask
-dontwarn org.osgi.framework.**

# ASM bytecode manipulation
-dontwarn org.objectweb.asm.**

# ===============================================
# COMPREHENSIVE APACHE POI RULES
# ===============================================

# Keep ALL Apache POI classes - most important rule
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# Keep POI OOXML classes
-keep class org.apache.poi.xwpf.** { *; }
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.poi.openxml4j.** { *; }

# Keep XML Beans (critical for POI OOXML)
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class schemaorg_apache_xmlbeans.** { *; }
-dontwarn schemaorg_apache_xmlbeans.**

# Keep generated XML Beans classes (POI generates these at runtime)
-keep class org.openxmlformats.** { *; }
-dontwarn org.openxmlformats.**
-keep class org.apache.poi.schemas.** { *; }
-dontwarn org.apache.poi.schemas.**

# StAX (Streaming API for XML) - critical for XML processing
-keep class javax.xml.stream.** { *; }
-dontwarn javax.xml.stream.**
-keep class com.ctc.wstx.** { *; }
-dontwarn com.ctc.wstx.**
-keep class org.codehaus.stax2.** { *; }
-dontwarn org.codehaus.stax2.**

# DOM4J classes (XML processing library used by POI)
-keep class org.dom4j.** { *; }
-dontwarn org.dom4j.**

# Prevent obfuscation of reflection-accessed classes
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# Keep all constructors for POI classes
-keepclassmembers class org.apache.poi.** {
    <init>(...);
    public <init>(...);
}

# Keep all public methods and fields
-keepclassmembers class org.apache.poi.** {
    public *;
    protected *;
}

# Keep enums (POI uses many enums)
-keepclassmembers enum org.apache.poi.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Specific XWPF classes we use directly
-keep class org.apache.poi.xwpf.usermodel.XWPFDocument {
    public <init>();
    public <init>(...);
    public *** createParagraph();
    public void write(...);
    public void close();
    public static *** PICTURE_TYPE_JPEG;
    public static *** PICTURE_TYPE_PNG;
}

-keep class org.apache.poi.xwpf.usermodel.XWPFParagraph {
    public *** createRun();
    public void setAlignment(...);
    public *** getAlignment();
}

-keep class org.apache.poi.xwpf.usermodel.XWPFRun {
    public void setText(...);
    public void setFontSize(...);
    public void setItalic(...);
    public void setBold(...);
    public void addPicture(...);
    public void addBreak();
    public void setColor(...);
}

-keep class org.apache.poi.xwpf.usermodel.ParagraphAlignment {
    public static *** CENTER;
    public static *** LEFT;
    public static *** RIGHT;
    public static *** JUSTIFY;
}

# Keep POI utility classes
-keep class org.apache.poi.util.Units {
    public static *** toEMU(...);
    public static *** toPoints(...);
}

# ===============================================
# DOCX CREATOR SPECIFIC RULES
# ===============================================

# Keep our DocxCreator class
-keep class com.example.kropimagecropper.utils.DocxCreator {
    public *;
    private *;
}

# Keep bitmap classes
-keep class android.graphics.Bitmap { *; }
-keep class android.graphics.BitmapFactory { *; }
-keep class android.graphics.BitmapFactory$Options { *; }
-keep class android.graphics.Bitmap$CompressFormat {
    public static *** JPEG;
    public static *** PNG;
}

# ===============================================
# ADDITIONAL SAFETY RULES
# ===============================================

# Keep all classes that extend InputStream/OutputStream
-keep class * extends java.io.InputStream { *; }
-keep class * extends java.io.OutputStream { *; }
-keep class java.io.ByteArrayInputStream { *; }
-keep class java.io.ByteArrayOutputStream { *; }
-keep class java.io.FileOutputStream { *; }

# Keep reflection-related classes
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Additional XML processing warnings
-dontwarn org.apache.xerces.**
-dontwarn org.w3c.dom.**
-dontwarn javax.xml.bind.**
-dontwarn javax.xml.parsers.**
-dontwarn org.xml.sax.**

# Apache Commons (if used by POI)
-dontwarn org.apache.commons.**
-keep class org.apache.commons.** { *; }

# Log4j (POI might use it)
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**

# Additional Apache POI warnings
-dontwarn org.apache.poi.ss.formula.**
-dontwarn org.apache.poi.hssf.**
-dontwarn org.apache.poi.xssf.**
-dontwarn org.apache.poi.hwpf.**
-dontwarn org.apache.poi.hslf.**
-dontwarn org.apache.poi.hdgf.**
-dontwarn org.apache.poi.hpbf.**
-dontwarn org.apache.poi.hmef.**

# ===============================================
# SIZE OPTIMIZATION RULES
# ===============================================

# Enable aggressive optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove logging in release builds (significant size reduction)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove debug code
-assumenosideeffects class * {
    void debug*(...);
    void trace*(...);
}

# Remove unused OpenCV modules (comment out if you need them)
-dontwarn org.opencv.calib3d.**
-dontwarn org.opencv.contrib.**
-dontwarn org.opencv.features2d.**
-dontwarn org.opencv.ml.**
-dontwarn org.opencv.objdetect.**
-dontwarn org.opencv.video.**
-dontwarn org.opencv.dnn.**
-dontwarn org.opencv.photo.**

# Remove unused POI modules for DOCX-only usage
-dontwarn org.apache.poi.hssf.**     # Excel .xls support
-dontwarn org.apache.poi.xssf.**     # Excel .xlsx support
-dontwarn org.apache.poi.hwpf.**     # Word .doc support
-dontwarn org.apache.poi.hslf.**     # PowerPoint support
-dontwarn org.apache.poi.hdgf.**     # Visio support
-dontwarn org.apache.poi.hpbf.**     # Publisher support

# Remove unused Markwon modules
-dontwarn io.noties.markwon.ext.**
-dontwarn io.noties.markwon.image.**
-dontwarn io.noties.markwon.linkify.**
-dontwarn io.noties.markwon.recycler.**

# Kotlin optimization
-dontwarn kotlinx.coroutines.debug.**
-dontwarn kotlinx.serialization.**

# Remove unused resources
-keepclassmembers class **.R$* {
    public static <fields>;
}
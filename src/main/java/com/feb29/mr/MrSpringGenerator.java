package com.feb29.mr;

import static io.swagger.codegen.CodegenConstants.API_PACKAGE;
import static java.io.File.separator;
import static java.io.File.separatorChar;

import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.languages.SpringCodegen;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extension to the SpringCodegen to generate maestro spring-boot service
 */
public class MrSpringGenerator extends SpringCodegen
{

    private static final String DEPLOY_DIR = "deploy";
    private static final String OPERATION = "operation";
    protected Map<String, Object> maestroOperations = new HashMap<>();
    protected Set<String> imports = new HashSet<>();

    private String apiModule;
    private String implModule;
    private String wsModule;


    public MrSpringGenerator()
    {
        super();
        outputFolder = "generated-code/mr-spring";
        apiTestTemplateFiles.clear();
        embeddedTemplateDir = templateDir = "mr-spring";
        artifactId = "maestro";
        groupId = "com.feb29.mr.mr";

        additionalProperties.put(JAVA_8, "true");
        maestroOperations.put(OPERATION, new ArrayList<CodegenOperation>());
    }


    @Override
    public String getName()
    {
        return "mr-spring";
    }


    @Override
    public String getHelp()
    {
        return "Generates a Java SpringBoot Server application using the SpringFox integration for GE maestro services.";
    }


    @Override
    public void processOpts()
    {
        super.processOpts();
        basePackage = groupId + sanitizePackageName('.' + artifactId);
        additionalProperties.put(BASE_PACKAGE, basePackage);
        apiPackage = basePackage + ".api";
        additionalProperties.put(API_PACKAGE, apiPackage);
        modelPackage = basePackage + ".model";
        additionalProperties.put(API_PACKAGE, modelPackage);
        configPackage = basePackage + ".ws";
        additionalProperties.put(CONFIG_PACKAGE, configPackage);
        apiModule = artifactId + "-api" + separator;
        implModule = artifactId + "-impl" + separator;
        wsModule = artifactId + "-ws" + separator;
        Calendar calendar = Calendar.getInstance();
        additionalProperties.put("year", calendar.get(Calendar.YEAR));
        String serviceName = toApiName(artifactId);
        additionalProperties.put("serviceName", serviceName);

        // build files
        supportingFiles.add(new SupportingFile("build-gradle.mustache", "", "build.gradle"));
        supportingFiles.add(new SupportingFile("settings-gradle.mustache", "", "settings.gradle"));
        supportingFiles.add(new SupportingFile("imake-file.mustache", DEPLOY_DIR, "Imakefile"));
        supportingFiles.add(new SupportingFile("build-gradle-deploy.mustache", DEPLOY_DIR, "build.gradle"));

        // deploy related files
        supportingFiles.add(new SupportingFile("service-jar.mustache", DEPLOY_DIR, artifactId + "-boot.jar.txt"));

        supportingFiles.add(new SupportingFile("init.mustache", DEPLOY_DIR, "MR" + serviceName + ".init"));
        supportingFiles.add(new SupportingFile("runService-sh.mustache", DEPLOY_DIR, "run-" + artifactId + ".sh"));
        supportingFiles.add(new SupportingFile("2product.mustache", DEPLOY_DIR, "toProduct"));

        // docker build files
        String dockerFolder = artifactId + "-docker";
        supportingFiles.add(new SupportingFile("dockerFile.mustache", dockerFolder, "Dockerfile"));
        supportingFiles.add(new SupportingFile("buildService-sh.mustache", dockerFolder,
                                               "build-" + artifactId + ".sh"));
        supportingFiles.add(new SupportingFile("buildServiceInDocker-sh.mustache", dockerFolder,
                                               "build-" + artifactId + "-in-docker.sh"));


        if (!interfaceOnly)
        {
            String srcFolder = sourceFolder + separator;
            String tstFolder = testFolder + separator;
            String wsConfigPkg = wsModule + srcFolder + configPackage;
            String resources = "src.main.resources";
            String aopPkg = implModule + srcFolder + basePackage + ".aop";
            String aopTestPkg = implModule + tstFolder + basePackage + ".aop";
            String cukePkg = testFolder + '.' + basePackage;
            String testResources = "src.test.resources.";
            String testResourcePkg = testResources + basePackage;

            if (System.getProperty("apis") == null)
            {
                apiTemplateFiles.put("swagger2SpringBoot.mustache", "Application.java");
                apiTemplateFiles.put("apiImpl.mustache", "Impl.java");
                apiTemplateFiles.put("apiException.mustache", "Exception.java");
                apiTemplateFiles.put("exceptionHandler.mustache", "ExceptionHandler.java");
                apiTemplateFiles.put("applicationTest.mustache", "ApplicationTest.java");
                apiTemplateFiles.put("implStepDefs.mustache", "StepDefs.java");
                apiTemplateFiles.put("restStepDefs.mustache", "RestStepDefs.java");
            }

            supportingFiles.add(new SupportingFile("homeController.mustache",
                                                   wsConfigPkg.replace(".", separator), "SwaggerController.java"));
            supportingFiles.add(new SupportingFile("application.mustache",
                                                   (wsModule + resources).replace(".", separator),
                                                   "application.properties"));
            supportingFiles.add(new SupportingFile("swaggerDocumentationConfig.mustache",
                                                   wsConfigPkg.replace(".", separator),
                                                   "SwaggerDocumentationConfig.java"));
            supportingFiles.add(new SupportingFile("aopLogging.mustache",
                                                   aopPkg.replace(".", separator), "LoggingAspect.java"));
            supportingFiles.add(new SupportingFile("logback.mustache",
                                                   (implModule + resources).replace(".", separator), "logback.xml"));
            //test files
            supportingFiles.add(new SupportingFile("aopLoggingTest.mustache",
                                                   (aopTestPkg).replace(".", separator),
                                                   "LoggingAspectTest.java"));
            supportingFiles.add(new SupportingFile("implRunCukes.mustache",
                                                   (implModule + cukePkg + ".impl").replace(".", separator),
                                                   "RunCukes.java"));
            supportingFiles.add(new SupportingFile("restRunCukes.mustache",
                                                   (wsModule + cukePkg + ".ws").replace(".", separator),
                                                   "RunCukes.java"));
            supportingFiles.add(new SupportingFile("impl-feature.mustache",
                                                   (implModule + testResourcePkg + ".impl").replace(".", separator),
                                                   artifactId + "-unit.feature"));
            supportingFiles.add(new SupportingFile("rest-feature.mustache",
                                                   (wsModule + testResourcePkg + ".ws").replace(".", separator),
                                                   artifactId + "-rest.feature"));
            supportingFiles.add(new SupportingFile("logbackTest.mustache",
                                                   (implModule + testResources).replace(".", separator),
                                                   "logback.xml"));
        }

        Iterator<SupportingFile> iterator = supportingFiles.iterator();
        while (iterator.hasNext())
        {
            SupportingFile file = iterator.next();
            if ("pom.xml".equals(file.destinationFilename) || file.folder.contains("io/swagger") ||
                    ("application.properties".equals(file.destinationFilename) && !file.folder.startsWith(artifactId)))
            {
                // REMOVE SpringCodegen generated files but not required for maestro codegen
                iterator.remove();
            }
        }
    }


    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs)
    {
        Map<String, Object> springObjs = super.postProcessOperations(objs);
        Map<String, Object> springOperations = (Map<String, Object>) springObjs.get("operations");
        if (springOperations != null)
        {
            List<CodegenOperation> springOps = (List<CodegenOperation>) springOperations.get(OPERATION);
            for (CodegenOperation operation : springOps)
            {
                if ("Void".equals(operation.returnType))
                {
                    operation.returnType = "void";
                }
            }
            List<CodegenOperation> mrOps = (List<CodegenOperation>) maestroOperations.get(OPERATION);
            if (!springOps.isEmpty())
            {
                mrOps.addAll(springOps);
            }
        }
        springObjs.put("classname", toApiName(artifactId));
        springObjs.put("pathPrefix", toApiVarName(artifactId));
        springObjs.put("operations", maestroOperations);

        List list = (List) springObjs.get("imports");
        imports.addAll(list);
        springObjs.put("imports", imports);

        return springObjs;
    }


    @Override
    public String toApiName(String name)
    {
        if (!artifactId.equals(name))
        {
            String message = String.format("Using artifactId:%s instead of endpoint root:%s .", artifactId, name);
            LOGGER.warn(message);
        }
        String serviceName = sanitizeName(artifactId);
        return camelize(serviceName);
    }


    @Override
    public String toApiVarName(String name)
    {
        return camelize(artifactId, true);
    }


    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder()
    {
        return outputFolder + separator + apiModule + sourceFolder + separator + apiPackage()
                .replace('.', separatorChar);
    }


    /**
     * Location to write model files.  You can use the modelPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String modelFileFolder()
    {
        return outputFolder + separator + apiModule + sourceFolder + separator + modelPackage()
                .replace('.', separatorChar);
    }


    /**
     * Location to write implementation files.
     */
    private String implFileFolder()
    {
        return outputFolder + separator + implModule + sourceFolder + separator + basePackage
                .replace('.', separatorChar) + separator + "impl";
    }


    /**
     * Location to write controller files.
     */
    private String controllerFileFolder()
    {
        return outputFolder + separator + wsModule + sourceFolder + separator + configPackage
                .replace('.', separatorChar);
    }


    private String baseFolder(String module)
    {
        return outputFolder + separator + artifactId + '-' + module + separator + sourceFolder + separator + basePackage
                .replace('.', separatorChar);
    }

    // test folders

    private String baseTestFolder(String module)
    {
        return outputFolder + separator + artifactId + '-' + module + separator + testFolder + separator + basePackage
                .replace('.', separatorChar);
    }

    /**
     * Location to write implementation files.
     */
    private String implTestFileFolder()
    {
        return outputFolder + separator + implModule + testFolder + separator + basePackage.replace('.', separatorChar)
                + separator + "impl";
    }


    /**
     * Location to write controller files.
     */
    private String controllerTestFileFolder()
    {
        return outputFolder + separator + wsModule + testFolder + separator + configPackage.replace('.', separatorChar);
    }


    @Override
    public String apiFilename(String templateName, String tag)
    {
        String suffix = apiTemplateFiles().get(templateName);
        String folder = apiFileFolder();
        if (suffix.equals("Controller.java"))
        {
            folder = controllerFileFolder();
        }
        else if (suffix.equals("Impl.java"))
        {
            folder = implFileFolder();
        }
        else if (suffix.equals("Application.java"))
        {
            folder = baseFolder("ws");
        }
        else if (suffix.equals("Exception.java"))
        {
            folder = baseFolder("api") + separator + "exception";
        }
        else if (suffix.equals("ExceptionHandler.java"))
        {
            folder = baseFolder("ws") + separator + "handler";
        }
        else if (suffix.equals("ApplicationTest.java"))
        {
            folder = baseTestFolder("ws");
        }
        else if (suffix.equals("StepDefs.java"))
        {
            folder = implTestFileFolder();
        }
        else if (suffix.equals("RestStepDefs.java"))
        {
            folder = controllerTestFileFolder();
        }
        return folder + '/' + toApiFilename(tag) + suffix;
    }


    private static String sanitizePackageName(String packageName)
    {
        if (packageName == null || packageName.trim().isEmpty())
        {
            return "invalidPackageName";
        }
        String pkgName = packageName.trim();
        pkgName = pkgName.replaceAll("[^a-zA-Z0-9_\\.]", "-");
        return pkgName;
    }


}
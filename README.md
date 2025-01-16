# **Spring context dotenv loader**

[![test](https://github.com/sysrio/springcontext-env/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/sysrio/springcontext-env/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.sysr/springcontext-env.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.sysr/springcontext-env)

`springcontext-env` is a lightweight, no-dependency (`no deps`) environment variables loader for Spring applications. It can be used in both standalone Spring applications or Spring Boot-powered applications. By _Spring standalone_, we mean applications that do not use Spring Boot auto-configurations.

This library loads dotenv variables or properties from `.env` files. It allows you to define several `.env` files for your application configurations—for example, `.env-dev`, `.env-prod`, `.env-test`, etc. Essentially, the library will look for any files that start with `.env`. However, it will not do this automatically unless the `.env` files are located at the root directory of the project.

There are several ways to tell the library where to look for your `.env` files:

1. **Add a file called `dotenv.properties` in your project's resources folder**.

   This properties file will specify a full directory path that contains the `.env` files, and an (optional) list of actual file names you would like to load and use within your application.

   - If you don't specify the actual `.env` files to load, the library will, by default, load all the available files in the provided directory that start with `.env`.
   - If the filenames to be used are specified in `dotenv.properties`, then only those files will be loaded. In this case, you can name your `env` files any name you want; they don't have to be prefixed with `.env`.

   **Example `dotenv.properties` file content**

   ```properties
   ENV_DIR_PATH=L:\Lab\apps\config
   FILE_1=test.config
   FILE_2=prod
   ```

   **OR**

   ```properties
   ENV_DIR_PATH=L:\Lab\apps\config
   FILE_A=test.config
   FILE_B=prod
   ```

   **OR**

   ```properties
   ENV_DIR_PATH=L:\Lab\apps\config
   FILE_NAME=.env-dev
   ```

   - Remember that file name in the `dotenv.properties` must start with **`FILE_`** followed by any letter or names you like. The variable `ENV_DIR_PATH` is case sensitive in UNIX. We recommend you maintain the uppercasing format.

2. **Add a system environment variable**

   - `ENV_DIR_PATH` in your system. You can specify to this library where you what it to load the `.env` files from by adding an _ENV_DIR_PATH_ in your system. Follow your spefic system intstructions on how to add an environment variable.

   - Ensure that the directory path you specified is a full path and can be accessed by the application that uses this library. Again any files in the provided directory that starts with `.env` will be loaded. You can override this behaviour by specifying the files you want loaded in the `dotenv.properties`.

   - **Examples**
     - Unix:
       ```properties
       export ENV_DIR_PATH=/home/var/config
       ```
     - Windows:
       ```properties
       SET ENV_DIR_PATH=L:\Lab\apps\config
       ```

3. **Add .env files in the project root directory**.

   - This is the default behavior, and the library will look for any `.env` files in the project sources root dir. This is mainly for development or test purposes.

   - However, it is recommended to use the first two options when creating a production-grade app. The above two options assume that your env files are somewhere in your file system and not in the project source code files.

     - If you are using this default behavior, ensure to add your `.env` files in the `.gitignore` so that they don't get pushed into the source control system.

# **Integration**

- For `Spring Boot` applications, just include this library as part of your project and you are done; no configurations needed.

- For the `Non-Spring Boot` applications, you will need to do a little configuration; you will need to add an initializer in your application. The following is a configuration for non-Spring Boot apps.

**Non-Web Spring applications**

```properties
    @Configuration
    public class MyConfiguration {
        @Bean
        public static PropertySourcesPlaceholderConfigurer placeholderConfigurer(){
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    public class Main(String []args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            // Initialize the SpringContext Env Loader Application context initializer
            SpringContextEnvApplicationContextInitializer initializer = new SpringContextEnvApplicationContextInitializer();
            initializer.initialize(context);

            // Now register your classes
            context.register(MyConfiguration.class);
            context.refresh();
        }
    }
```

**Spring web applications**

```properties
    @Configuration
    @EnableWebMvc
    @ComponentScan(basePackages = "com")
    public class AppConfig {
        // Your beans and other configurations
    }

    public class MyWebAppInitializer implements WebApplicationInitializer {
        @Override
        public void onStartup(@NonNull ServletContext servletContext) throws ServletException {
            AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
            SpringContextEnvApplicationContextInitializer env = new SpringContextEnvApplicationContextInitializer();
            env.initialize(context);
        }
    }
```

- Remember that `WebApplicationInitializer` implementations are detected automatically. So, once you define one, you are good to go.

# **Implementation Considerations**

The following are some of the considerations that we have made:

- Incomplete definitions are ignored.

  - The tooling will ignore incomplete defined variables. An example of incomplete variables include:

  ```props
  KEY
  KEY=
  KEY=${}
  KEY=${  }
  KEY=${
  ""
  ```

  However, when there is an existing **System Variable** defined with such `KEY`, then the value of the system variable will be used. So, you are free to define a system variable and tell the library to load it by including it in the `.env` file.

  This tooling reguard `${}` or `${  }` as an empty variable value and therefore, ignored. The `${` is consired a bad variable defintion format and therefore, ignored.

  Also, we are of an opinion that it is not a good practice to have your configurations use empty passwords. So, if you define something like this:

  ```props
  DB_NAME=test
  USERNAME=doe
  PASSWORD=""
  ```

  The `PASSWORD` veriable will not be loaded, it will be ignored. That means if your configurations rely on the loaded `.env` properties then `PASSWORD` property will not be accessible because it is not loaded. The logs will tell you that such property is not loaded because it is not defined.

- Circular dependecies:
  - When there is a circular dependency amongst the variables in `.env` file, then the application will immediately halt by throwing an EnvContextLoaderException. A circular dependency looks like this:
  ```circular
  KEY=VAR
  VAR=${KEY}
  ```
- Property definitions:
  - There are several ways you can define your variables in the `.env` files:
  1. Space or Tab separated
     ```
     KEY VALUE
     KEY     VALUE
     ```
  2. Assignment Operator
     ```
     KEY=VALUE
     KEY =   VALUE
     ```
  3. Collon separated
     ```
     KEY:VALUE
     KEY :   VALUE
     ```
  4. Variable referencing
     ```
     USERNAME=jdoe
     PASSWORD=p@word
     DB_NAME=test
     HOST=localhost
     PORT=3306
     URL=jdbc:mysql://${HOST}:${PORT}/${DB_NAME}
     ```
- Under the hood we are using `java.util.Properties` API to parse the `.env` files. The files should be `UTF-8` compatible.

# **Usage**

Add the tooling to your application dependecies. The easiest way is to source from mave central repository and add to your build tool. The current version is **`1.0.3`**

- Using gradle
  ```
    implementation group: 'io.sysr', name: 'springcontext-env', version: '1.0.3'
  ```
- Using Apache Maven
  ```
  <dependency>
     <groupId>io.sysr</groupId>
     <artifactId>springcontext-env</artifactId>
     <version>1.0.3</version>
  </dependency>
  ```
- **Remember:** For **`Spring boot`** applications, only include this library as part of your project. No additional configurations are needed. It will integrate seamlessly with your application.

**Note:** It's important to keep your `.env` files secure, as they may contain sensitive information like API keys and passwords. These files should not be committed to source control systems like Git to prevent exposing sensitive data.

# **Containerized Applications**

- For containerized applications, you can manage environment variables efficiently by using a shared volume. Here’s how you can set it up:

  1. **Define ENV_DIR_PATH:**

     - Ensure that your container knows about an optional `ENV_DIR_PATH` variable that the user can provide when starting the container. Remember that `ENV_DIR_PATH` points at the directory where your `.env` files are located. It should not point at your `.env` file.

  2. **Example Docker Configuration:**

     ```dockerfile
     # Dockerfile

     # Use a base image
     FROM openjdk:21-jdk-slim

     # Copy the application files
     COPY target/myapp.jar /app/myapp.jar

     # Define the ENV_DIR_PATH environment variable
     ENV ENV_DIR_PATH=/path/to/env

     # Run the application
     CMD ["sh", "-c", "source $ENV_DIR_PATH/.env && java -jar /app/myapp.jar"]
     ```

# **Why Use this Dotenv Loader Library**

- **Centralized Management**

  - This env loader library provides a single, consistent place to define and manage environment variables, making it easier to maintain and update configurations.

- **Security**

  - By loading environment variables from a secure location, you can avoid hardcoding sensitive information like API keys and passwords directly in your codebase, reducing the risk of accidental exposure.

- **Portability**

  - Environment variables can be easily ported across different environments (development, testing, production) without changes to the application code. This ensures consistency and reduces configuration errors.

- **Ease of Use**
  - The library easily integrates with your spring boot application. No configurations needed.

# **Contributing**

If you would like to contribute to the continual maintaince and improvement of this tooling, please refer to the <a href="[CONTRIBUTING.MD]">CONTRIBUTING.MD</a> for more information.

## **Buy Me Coffee**

You are happy with the work done so far? Please remember to support the creative minds behind this tooling by sharing with them some coffee <a href="https://buymeacoffee.com/sysr">Sponsor</a>

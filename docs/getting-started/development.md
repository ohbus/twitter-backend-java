---
title: Development environment
description: Configure an editor and use the standard Maven development commands.
---

# Development environment

Import the repository as a Maven project and configure the editor to use JDK
25. Annotation processing must be enabled for MapStruct. Lombok is not used.

Useful commands:

```bash
./mvnw test
./mvnw spotless:apply
./mvnw clean verify
./mvnw jacoco:report
```

Run the application with the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Use imports instead of fully qualified names and preserve the
Controller → Service → Repository layering described in the
[architecture overview](../architecture/README.md).

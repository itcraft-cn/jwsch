# Maven依赖设计

## 一、版本管理

### 1.1 版本号

当前版本：`1.0.0-SNAPSHOT`

### 1.2 依赖版本

```xml
<properties>
    <netty.version>4.2.0.Final</netty.version>
    <guava.version>31.1-jre</guava.version>
    <slf4j.version>1.7.36</slf4j.version>
    <logback.version>1.2.12</logback.version>
    <junit.version>4.13.2</junit.version>
    <mockito.version>3.12.4</mockito.version>
</properties>
```

---

## 二、jwsch-common依赖

```xml
<dependencies>
    <!-- Netty -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>${netty.version}</version>
    </dependency>
    
    <!-- Guava (MurmurHash3) -->
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>${guava.version}</version>
    </dependency>
    
    <!-- SLF4J -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>${slf4j.version}</version>
    </dependency>
    
    <!-- Test: Logback -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Test: JUnit -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>${junit.version}</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Test: Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>${mockito.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 三、jwsch-cli依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.itcraft</groupId>
        <artifactId>jwsch-common</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

---

## 四、jwsch-srv依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.itcraft</groupId>
        <artifactId>jwsch-common</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>cn.itcraft</groupId>
        <artifactId>jwsch-cli</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

---

## 五、可观测性依赖（Phase 2+）

### 5.1 Micrometer

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.9.0</version>
</dependency>
```

### 5.2 OpenTelemetry

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
    <version>1.17.0</version>
</dependency>
```

---

## 六、父POM

```xml
<project>
    <groupId>cn.itcraft</groupId>
    <artifactId>jwsch</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>jwsch-common</module>
        <module>jwsch-cli</module>
        <module>jwsch-srv</module>
    </modules>
    
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        
        <netty.version>4.2.0.Final</netty.version>
        <guava.version>31.1-jre</guava.version>
        <slf4j.version>1.7.36</slf4j.version>
        <logback.version>1.2.12</logback.version>
        <junit.version>4.13.2</junit.version>
        <mockito.version>3.12.4</mockito.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.netty</groupId>
                <artifactId>netty-all</artifactId>
                <version>${netty.version}</version>
            </dependency>
            <dependency>
                <groupId>com.google.guava</groupId>
                <artifactId>guava</artifactId>
                <version>${guava.version}</version>
            </dependency>
            <dependency>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-api</artifactId>
                <version>${slf4j.version}</version>
            </dependency>
            <dependency>
                <groupId>ch.qos.logback</groupId>
                <artifactId>logback-classic</artifactId>
                <version>${logback.version}</version>
            </dependency>
            <dependency>
                <groupId>junit</groupId>
                <artifactId>junit</artifactId>
                <version>${junit.version}</version>
            </dependency>
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-core</artifactId>
                <version>${mockito.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---
package com.demo.bytebuddytest;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@SpringBootApplication
public class BytebuddytestApplication {

    public static void main(String[] args) throws Exception {
        // subclass();
        // rebase();
        // redefine();

        byteBuddyAgent();

        //SpringApplication.run(BytebuddytestApplication.class, args);
    }


    //region Description

    private static void subclass() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // 使用ByteBuddy创建一个新的类
        DynamicType.Unloaded<?> unloadedClass = new ByteBuddy()
                // 父类为Object
                .subclass(Object.class)
                // 设置类名
                .name("com.demo.bytebuddytest.MyClass")
                // 定义一个名为"hello"，返回类型为 String，可见性为 public 的方法
                .defineMethod("hello", String.class, Visibility.PUBLIC)
                // hello 方法参数为一个 String
                .withParameters(String.class)
                // hello 方法体使用 MyClassImplementation 的 hello 方法
                // 可以方法名相同 或者 参数列表能够唯一匹配
                .intercept(MethodDelegation.to(new MyClassImplementation()))
                // 匹配 toString 方法
                .method(ElementMatchers.named("toString"))
                // toString 方法返回固定的值 "com.demo.bytebuddytest.MyClass!"
                .intercept(FixedValue.value("com.demo.bytebuddytest.MyClass"))
                // 生成类的字节码
                .make();

        // 加载并实例化新生成的类
        Class<?> dynamicClass = unloadedClass.load(BytebuddytestApplication.class.getClassLoader())
                .getLoaded();
        Object instance = dynamicClass.newInstance();

        // 找到 hello 方法
        Method method = dynamicClass.getMethod("hello", String.class);
        // 调用新生成的方法
        String result = (String) method.invoke(instance, "ByteBuddy");
        // 输出 "Hello, ByteBuddy"
        System.out.println(result);
    }

    private static void rebase() throws IllegalAccessException, InvocationTargetException {
        String className = "com.demo.bytebuddytest.OriginalClass";
        TypePool typePool = TypePool.Default.of(BytebuddytestApplication.class.getClassLoader());
        TypeDescription typeDescription = typePool.describe(className).resolve();
        ClassFileLocator locator = ClassFileLocator.ForClassLoader.of(BytebuddytestApplication.class.getClassLoader());

        new ByteBuddy()
                .rebase(typeDescription, locator)
                .method(ElementMatchers.named("getString"))
                .intercept(FixedValue.value("bar"))
                .make()
                .load(
                        BytebuddytestApplication.class.getClassLoader(),
                        ClassLoadingStrategy.Default.INJECTION
                );

        OriginalClass originalClass = new OriginalClass();
        // bar: result
        String result = originalClass.getString("qq");
        System.out.println(result); // output


        Method[] ms = OriginalClass.class.getDeclaredMethods();
        Method originGetString =Arrays.stream(ms)
                .filter(x->x.getName().startsWith("getString$original"))
                .findFirst()
                .get();
        originGetString.setAccessible(true);
        // originResult: foo world
        String originResult = (String) originGetString.invoke(originalClass, "world");
        System.out.println(originResult);
    }

    private static void redefine() {
        String className = "com.demo.bytebuddytest.OriginalClass";
        TypePool typePool = TypePool.Default.of(BytebuddytestApplication.class.getClassLoader());
        TypeDescription typeDescription = typePool.describe(className).resolve();
        ClassFileLocator locator = ClassFileLocator.ForClassLoader.of(BytebuddytestApplication.class.getClassLoader());

        new ByteBuddy()
                .redefine(typeDescription, locator)
                .method(ElementMatchers.named("getString"))
                .intercept(FixedValue.value("bar"))
                .make()
                .load(
                        BytebuddytestApplication.class.getClassLoader(),
                        ClassLoadingStrategy.Default.INJECTION
                );

        OriginalClass originalClass = new OriginalClass();
        // bar: result
        String result = originalClass.getString("qq");
        System.out.println(result);
    }

    //endregion

    private static void byteBuddyAgent() {
        // 初始化 Byte Buddy Agent
        Instrumentation instrumentation = ByteBuddyAgent.install();
        new AgentBuilder.Default()
                // 通过注解进行增强
                //.type(ElementMatchers.isAnnotatedWith(Intercept.class))
                .type(ElementMatchers.named("com.demo.bytebuddytest.OriginalClass"))
                .transform( (builder, typeDescription, classloader, module, protectionDomain)-> {
                    return builder.method(ElementMatchers.named("getString"))
                            .intercept(MethodDelegation.to(OriginalClassInterceptor.class));
                })
                .installOn(instrumentation);

        OriginalClass originalClass = new OriginalClass();
        String output = originalClass.getString("world");
        System.out.println(output); // 你好 world
    }

}

package io.pretty.hystrix;

import com.netflix.hystrix.*;

public class SimpleHystrixCommand extends HystrixCommand<Void> {

    private String url;

    private static final Setter setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("MyHystrixCommand")) // groupKey 对command进行分组, 用于 reporting, alerting, dashboards, or team/library ownership, 也是 ThreadPool 的默认 key
            .andCommandKey(HystrixCommandKey.Factory.asKey("hystrix")) // 可以根据 commandKey 具体的运行时参数
            .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("hystrix-thread"))  // 指定 ThreadPool key, 这样就不会默认使用 GroupKey
            .andCommandPropertiesDefaults(HystrixCommandProperties.Setter() // 初始化 Command 相关属性
                    .withMetricsRollingStatisticalWindowInMilliseconds(
                            100 * 1000) // 设置统计窗口为100秒
                    .withCircuitBreakerSleepWindowInMilliseconds(
                            10 * 1000) // 设置熔断以后, 试探间隔为10秒
                    .withCircuitBreakerRequestVolumeThreshold(
                            10) // 设置判断熔断请求阈值为10
                    .withCircuitBreakerErrorThresholdPercentage(
                            80) // 设置判断熔断失败率为80%
                    .withExecutionTimeoutInMilliseconds(3 * 1000)) // 设置每个请求超时时间为3秒
            .andThreadPoolPropertiesDefaults( // 设置和threadPool相关
                    HystrixThreadPoolProperties.Setter().withCoreSize(20)); // 设置 threadPool 大小为20(最大20个并发)

    public SimpleHystrixCommand(String url){
        this(setter,url);

    }

    public SimpleHystrixCommand(Setter setter, String url) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("MyHystrixCommand")));
        this.url = url;
    }

    @Override
    protected Void run() throws Exception {
        if (url.endsWith("a")){
            throw new RuntimeException("hehh");
//            Thread.sleep(5000);
        }
        return null;
    }

    @Override
    public Void getFallback() {
        System.out.println("服务调用失败");
        Throwable exception = getFailedExecutionException();
        if (null != exception){
            System.out.println(exception.getMessage());
            System.out.println("===");

        }
        System.out.println(getLogMessagePrefix());

        return null;
    }
}
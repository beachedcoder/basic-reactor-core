import domain.StockUnitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicTests {

    private List<String> dataValues;
    private Flux dataSource;
    private int dataValuesCount;

    @BeforeEach
    public void initTestData(){
        this.dataValues = Arrays.asList(
                "The","quick","brown","fox","jumps","over","the","lazy","dog"
        );
        this.dataSource = Flux.fromIterable(this.dataValues);
        this.dataValuesCount = this.dataValues.size();
    }


    @Test
    public void basicFluxSequenceIterativeTest(){
        StepVerifier.create(this.dataSource)
                .expectNextSequence(this.dataValues)
                .verifyComplete();
    }


    @Test
    void demoFluxPublishing(){
        Flux.just(this.dataValues).subscribe(System.out::println);
    }








    @Test
    public void basicSpecificStepValueTest(){
        StepVerifier.create(this.dataSource)
                .expectNext(this.dataValues.get(0))
                .expectNext(this.dataValues.get(1))
                .expectNext(this.dataValues.get(2))
                .expectNext(this.dataValues.get(3))
                .expectNext(this.dataValues.get(4),this.dataValues.get(5),this.dataValues.get(6))
                .expectNext(this.dataValues.get(7))
                .expectNext(this.dataValues.get(8))
                .verifyComplete();
    }









    @Test
    public void basicFluxStreamCountTest(){
        StepVerifier.create(this.dataSource)
                .expectNextCount(this.dataValuesCount)
                .verifyComplete();
    }




    @Test
    public void basicSubscriptionExample(){
        Flux<Integer> numberSeq = Flux.range(1,20);

        numberSeq.subscribe(e->System.out.printf("Value received %s%n",e));
    }

    @Test
    public void basicSubscriptionWithError(){
        Flux<Integer> numberSeq = Flux.range(1,20)
                .map( e -> {if (e== 8) {throw new RuntimeException("error on the eights");} return e;});


        numberSeq.subscribe( e -> System.out.printf("Value received %s%n",e),
                error -> System.err.println("Error Published:: " + error) );
    }

    @Test
    public void basicSubscriberWithErrorAndDoneHandler(){
        Flux<Integer> numberSeq = Flux.range(1,20);

        numberSeq.subscribe( e -> System.out.printf("Value received %s%n",e),
                error -> System.err.println("Error Published:: " + error),
                () -> System.out.println("Complete event published"));
    }

    @Test
    public void basicSubscriberWithErrorAndDoneAndCancellationHandler(){
        Flux<Integer> publishSeq = Flux.range(1,20).delayElements(Duration.ofSeconds(1));




        Disposable subscriptionRef =
                publishSeq.subscribe( e -> System.out.printf("Value received %s%n",e),
                        error -> System.err.println("Error Published:: " + error),
                        () -> System.out.println("Complete event published"));

        Disposable alSubcriptionRef =
         publishSeq.subscribe( e -> System.out.printf("Alt Value received %s%n",e),
                        error -> System.err.println("Error Published:: " + error),
                        () -> System.out.println("Altl Complete event published"));

        Runnable runnableTask = () -> {
            try {
                TimeUnit.SECONDS.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Canceling subscription");
            subscriptionRef.dispose();
        };
        Runnable altRunnableTask = () -> {
            try {
                TimeUnit.SECONDS.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Canceling subscription");
            subscriptionRef.dispose();
        };
        runnableTask.run();
        altRunnableTask.run();
    }


    @Test
    public void basicPublisherProgrammatically(){
        Flux<String> strPublisher = Flux.generate(
                AtomicInteger::new,
                (mutableInt,publishIt) -> {
                    publishIt.next(String.format(" on next value:: %s",mutableInt.getAndIncrement()));
                    if (mutableInt.get() == 17 ){ publishIt.complete(); }
                    return mutableInt;
                });

        strPublisher.subscribe(s -> System.out.printf("Subscriber received:: %s%n",s),
                e -> System.out.println("Error published:: "+e),
                () -> System.out.println("Complete notification sent!"));
    }




    @Test
    public void basicPublisherWithSchedulers(){
        Scheduler reactScheduler = Schedulers.newParallel("pub-parallel", 4);
        final Flux<String> phrasePublish =
                Flux.range(1,20)
                        .map( i -> 42 + i)
                        .publishOn(reactScheduler)
                        .map(m -> {
                            var v = Thread.currentThread().getName();
                            return String.format("%s value produced::%s",v,m);});

        Runnable r0 = ()-> phrasePublish.subscribe(
                n -> System.out.printf("subscriber recvd:: %s%n",n)
        );
        Runnable r1 = () -> phrasePublish.subscribe(
                n -> System.out.printf("subscriber recvd:: %s%n",n)
        );
        Runnable r2 = () -> phrasePublish.subscribe(
                n -> System.out.printf("subscriber recvd:: %s%n",n)
        );
        Runnable r3 = ()-> phrasePublish.subscribe(
                n -> System.out.printf("subscriber recvd:: %s%n",n)
        );

        r0.run();
        r1.run();
        r2.run();
        r3.run();
    }

    @Test
    public void basicSubscriptionExampleWithPublisherOnError(){
        Flux<String> strSeq = Flux.just(1,3,5,8,0)
                .map( e -> {
                    if (e==5) {throw new IllegalArgumentException("faux receipt");}
                    int v = 100;
                    return String .format("reported percentage::%s",v/e);
                })
                .onErrorReturn(e->e.getMessage().contains("faux"),"alternate static message 8-)")
                .onErrorReturn(ArithmeticException.class,"Dividing by zero is bad")
                .onErrorReturn("static fallback return value");

        strSeq.subscribe(e->System.out.printf("Subscriber received:: %s%n",e));
    }


    //TODO sort out passing current inputs to onErrorResume

    @Test
    public void publisherWithErrorOnResumeAlternativePath(){
        Flux<Integer> strSeq = Flux.just(12345,12346,12347,12348)
                .flatMap(sku->callSkuService(sku))
                .onErrorResume(e ->{ log(e); return Mono.just(-2); })
                .onErrorResume(IllegalArgumentException.class,e ->{log(e);return errorSkuService(e);});

        strSeq.subscribe(e-> System.out.printf("received::%s%n",e));
    }

    @Test
    public void publisherWithErrorOnContinueAlternativePath(){
        Flux<Integer> strSeq = Flux.just(12345,12346,12347,12348)
                .flatMap(sku->callSkuService(sku))
                .onErrorContinue((throwable, o) -> {
                    log(throwable);
                    cacheSkuService((Integer) o);
                })
                .onErrorContinue(IllegalArgumentException.class,(throwable, o) -> {
                    log(throwable);
                    skuEvalService((Integer) o);
                });

        strSeq.subscribe(e-> System.out.printf("received::%s%n",e),
                throwable -> System.out.println(throwable.getMessage()));
    }

    @Test
    public void publishErrorInformationWithinEvents(){
        Flux<Integer> stock = Flux.just(12345,12346,12347,12348)
                .flatMap(sku->callSkuService(sku))
                .onErrorMap(e -> new StockUnitException("failed to find",e));

        stock.subscribe(e -> System.out.printf("received::%s%n",e),
                err -> System.out.println(err.getMessage()));
    }


    public boolean logService(Throwable e){ return true;}
    public void log(Throwable err){System.out.println(err.getMessage());}
    public Flux<Integer> callSkuService(int lookUpSku){
//        if (lookUpSku == 12346) throw new ArithmeticException("math is fun");
        if (lookUpSku == 12347) throw new IllegalArgumentException("testing is fun");
        return Flux.just(lookUpSku);
    }
    public Flux<Integer> errorSkuService(Throwable e){ return Flux.just(-42);}
    public Flux<Integer> cacheSkuService(int cacheSku){ return Flux.just(cacheSku);}
    public Flux<Integer> skuEvalService(int cacheSku){ return Flux.just(cacheSku);}


}
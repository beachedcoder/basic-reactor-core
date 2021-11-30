import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FluxDemoTests {
    List<UUID> dataItems;

    @BeforeEach
    void initData(){
        this.dataItems = Arrays.asList(
                UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),UUID.randomUUID(),
                UUID.randomUUID(),UUID.randomUUID()
        );
    }

    @Test
    void StartingWithFlux(){
        Flux.just(UUID.randomUUID())
                .log()
                .subscribe();
    }

    @Test
    void StartingWithFluxEvents(){
        Flux.just(this.dataItems)
                .log()
                .subscribe();
    }

    @Test
    void FluxWithEventMessages(){
        Flux.fromIterable(this.dataItems)
                .log()
                .subscribe();
    }

    @Test
    void simpleMappingFluxWithEventMessages(){
        Flux.fromIterable(this.dataItems)
                .map(m-> String.format("%s::%s",
                        UUID.randomUUID().toString().substring(0,8),
                        m))
                .log()
                .subscribe();
    }
    @Test
    void simpleConversion(){
        Flux.fromIterable(this.dataItems)
                .flatMap(m->{
                    return Mono.just(
                            String.format("%s::%s",
                                    UUID.randomUUID().toString().substring(0,8),
                                    m));
                })
                .log()
                .subscribe();
    }

    @Test
    void simpleMappingThatGeneratesErrors(){
        Flux.range(1,100)
                .log()
                .flatMap(m->{
                    if (m==8) return Flux.error(new IOException("eights over eights"));
                    return Mono.just(UUID.randomUUID());
                })
                .subscribe(
                        e-> System.out.println("value::"+e),
                        err -> System.out.println("error::"+err),
                        ()-> System.out.println("completed successfully")
                );
    }

    @Test
    void simpleMappingGeneratesError(){
        Flux.range(1,100)
                .log()
                .flatMap(e->{
                    if (e==42)
                        return Flux.error(new IOException("publisher encounter error"));
                    return Mono.just(UUID.randomUUID());
                })
                .subscribe(
                        System.out::println,
                        e-> System.out.println("error msg::"+e),
                        ()-> System.out.println("completed sequence")
                );

    }

    @Test
    void publishDirectlyProgrammatically(){
        Flux.generate(AtomicInteger::new,
            (mutableInt,publishChannel)->{
            publishChannel.next(
               String.format("onNext::%s",mutableInt.getAndIncrement())
            );
            if (mutableInt.get()==14){
                publishChannel.complete();
            }
            return mutableInt;
            })
            .subscribe(
                    m->System.out.printf("Subscribed::%s%n",m),
                    e-> System.out.printf("Subscribed Error::%s%n",e.getMessage()),
                    ()->System.out.printf("Publishing Complete%n")
            );
    }

    @Test
    void publishDirectlyWithoutMutation(){
        Flux.generate(AtomicLong::new,(state, ch)->{
            long cState = state.getAndIncrement();
            ch.next(UUID.randomUUID());
            if (cState>42){
                ch.complete();
            }
            return state;
            })
            .subscribe(
                m-> System.out.printf("published::%s%n",m),
                e-> System.out.printf("error produced::%s%n",e.getMessage()),
                ()->System.out.println("channel completion")
            );
    }

    @Test
    void cancelPublisherChannel(){
        Flux channel = Flux.range(1,1000)
                .log()
                .delayElements(Duration.ofSeconds(2))
                .flatMap(m->{
                    return Mono.just(UUID.randomUUID());
                });

        Disposable subScriber =
                channel.subscribe(
                        m->System.out.printf("message::%s%n",m),
                        e->System.out.printf("error sent::%s%n",e),
                        ()->System.out.println("channel completion sent")
                );

        Runnable task = ()->{
            try {
                TimeUnit.SECONDS.sleep(24);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Canceling subscription to channel");
            subScriber.dispose();
        };

        task.run();
    }

    @Test
    void pubOnParallel(){
        Scheduler sch = Schedulers.newParallel("pub-parallel",4);
        final Flux ch = Flux.range(1,30)
                .publishOn(sch)
                .flatMap(m->{
                    return Flux.just(String.format("%s published::%s%n",
                            Thread.currentThread().getName(),
                            m));
                });
        Runnable task0 = ()->ch.subscribe(
                m->{System.out.printf("task 0 recvd::%s%n",m);}
        );Runnable task1 = ()->ch.subscribe(
                m->{System.out.printf("task 1 recvd::%s%n",m);}
        );Runnable task2 = ()->ch.subscribe(
                m->{System.out.printf("task 2 recvd::%s%n",m);}
        );Runnable task3 = ()->ch.subscribe(
                m->{System.out.printf("task 3 recvd::%s%n",m);}
        );
        task0.run();
        task1.run();
        task2.run();
        task3.run();
    }

    @Test
    void generateErrorWhilePublishing(){
        UUID fixedValue = UUID.randomUUID();
        System.out.println("fixed value for errors::" + fixedValue);
        Flux.range(1,100)
                .log()
                .flatMap(m->{
                    if (m%2==0){
                        int c = (int)Math.round(Math.random()*10);
                        switch (c){
                            case 1:
                                System.out.println("illegal argument fake");
                                return Flux.error(new IllegalArgumentException("faux argument"));
                            case 2:
                                System.out.println("arithmetic exception fake");
                                return Flux.error(new ArithmeticException("dividing by zero bad"));
                            case 3:
                                System.out.println("fake IOException");
                                return Flux.error(new IOException("faux read failure"));
                        }
                    }
                    return Flux.just(UUID.randomUUID());
                })
                .onErrorReturn(IllegalArgumentException.class,fixedValue)
                .onErrorReturn(ArithmeticException.class,fixedValue)
                .onErrorReturn(IOException.class,fixedValue)
                .subscribe(
                        m-> System.out.printf("recvd::%s%n",m),
                        e-> System.out.printf("error::%s%n",e),
                        ()-> System.out.printf("completed")
                );
    }

    @Test
    void errorPublishWithContinue(){
        Flux.generate(AtomicLong::new,
                (state,ch)->{
            state.getAndIncrement();
            ch.next(UUID.randomUUID());
            if (state.get()==21){ ch.error(new IllegalArgumentException("faux input error"));}
            if (state.get()>=42){ch.complete();}
            return state;
            })
                .onErrorResume((t)->{
                    System.out.println("error encountered::"+t.getMessage());
                    return Flux.fromIterable(this.dataItems);
                })
                .subscribe(
                        m-> System.out.printf("recvd::%s%n",m),
                        e-> System.out.printf("error::%s%n",e.getMessage()),
                        ()-> System.out.println("completed")
                );
    }

}

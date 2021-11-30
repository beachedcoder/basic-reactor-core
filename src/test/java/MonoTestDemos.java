import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class MonoTestDemos {

    @Test
    void MonoNotWorking(){
        Mono.just("demo").log();
    }
    @Test
    void MonoWorking(){
        Mono.just("demo").log().subscribe();
    }
    @Test
    void MonoEventsWorking(){
        Mono.just("demo").log()
                .doOnSubscribe(m->System.out.println("subscribed::"+m))
                .doOnRequest(r->System.out.println("requested::"+r))
                .doOnSuccess(c->System.out.println("completed::"+c))
                .subscribe(System.out::println);
    }

    @Test
    void EmptyMonoDemo(){
        Mono.empty().log().subscribe(System.out::println);
    }
    @Test
    void EmptyMonoMultipleArgumentSubscibeDemo(){
        Mono.empty().log()
                .subscribe(System.out::println,
                        null,
                        ()->System.out.println("empty completed"));
    }
    @Test
    void MonoErrorDemo(){
        Mono.error(new RuntimeException())
                .log()
                .subscribe(System.out::println,
                        null,
                        ()->System.out.println("empty completed"));
    }
    @Test
    void MonoErrorConsumerDemo(){
        Mono.error(new IOException())
                .log()
                .subscribe(System.out::println,
                        e-> System.out.println("error received::"+e),
                        ()->System.out.println("empty completed"));
    }

}

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.UUID;

public class MonoDemoTests {

    @Test
    void MonoFromStaticData(){
        Mono.just(UUID.randomUUID()).subscribe(System.out::println);

    }


    @Test
    void MonoEventRegistration(){
        Mono pubIt = Mono.just(UUID.randomUUID())
//                .log()
                .doOnSubscribe(s-> System.out.println("on subscription::"+s))
                .doOnRequest(r-> System.out.println("on request::"+r))
                .doOnSuccess(c-> System.out.println("on completion::"+c));

                pubIt.subscribe(m-> System.out.printf("value: %s%n",m));
    }



    @Test
    void MonoWithoutValue(){
        Mono.empty()
//                .log()
                .subscribe(v-> System.out.println("value::"+v),
                        e-> System.out.println(e),
                        ()-> System.out.println("completed"));
    }





    @Test
    void MonoErrorPublish(){
        Mono.error(new IOException("hello exception message"))
//                .log()
                .subscribe(System.out::println,
                        e-> System.out.println("error received::"+e.getMessage()),
                        ()-> System.out.println("completion event"));
    }
}

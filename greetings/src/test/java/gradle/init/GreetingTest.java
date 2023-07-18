
package gradle.init;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GreetingTest {

  @Test
  void shouldReturnHelloWorld() {
      assertEquals("Hello World",new Greetings().greetings());
  }
}

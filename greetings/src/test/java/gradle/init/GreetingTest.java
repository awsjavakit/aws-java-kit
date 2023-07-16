
package gradle.init;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GreetingTest {

  @Test
  void shouldReturnHelloWorld() {
      assertEquals("Hello World",new Greetings().greetings());
  }
}

Zaprojektuj komponent przetwarzający zadania o różnym priorytecie.
Obsługa zadania wymaga wysłania żądania HTTP do zewnętrznego serwisu A.
Zadania pobierane są z systemu kolejkowego Rabbit MQ i po przetworzeniu powinny zostać zatwierdzone w kolejce źródłowej (ACK).
Przetworzone zadania wraz z wynikami zapytania powinny zostać wysłane do kolejki wynikowej.

    Założenia i ograniczenia:

        W serwisie A dostępna jest globalna pula zapytań wynosząca 100k/m-c. Przekroczenie limitu sygnalizowane jest błędem HTTP 403 Forbidden.
        Serwis A narzuca również rate limiting, którego wartość nie jest znana z góry. Przekroczenie progu sygnalizowane jest błędem HTTP 429 Too many requests.
        Zadania o priorytecie 1 powinny zużywać średnio 50% zapytań do serwisu zewnętrznego; priorytet 2 -> 30%; reszta pozostałe 20%


Określ i sformułuj dodatkowe założenia konieczne do implementacji rozwiązania.
Przygotuj prototyp komponentu zawierający logikę przetwarzania zadań (preferowanym językiem jest Scala, ale można wykorzystać inny z rodziny JVM).
Dodatkowym plusem będzie oparcie rozwiązania o Akka Streams.
Wymagana jest tylko logika przetwarzania, bez kontaktu z systemami zewnętrznymi (system kolejkowy i serwis zewnętrzny powinien zostać zamockowany).

-----------------------------------------------------------------------------------------------------------

Dodatkowe przyjęte założenia:



ZADANIE:

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

Dodatkowe przyjęte założenia (niektóre wynikają z treści zadadania - ale wolę spisać dodatkowo):

Liczba zadań jest potencjalnie nieograniczona.
Struktura wyniku zadania nie jest istotna, przyjmuje String.
Wyczerpanie globalnej puli zapytań spowoduje zatrzymanie flow (do momentu aż minie miesiąc).
Miesiąc to 30 dni i nie jest związany z miesiącem kalendarzowym (dla uproszczenia)
priorytet >=1

Rozwiązanie:

Korzystam z akka-streams, buduję flow który ma jedno wejście (taski) i dwa wyjścia (ACK dla kolejki wejściowej, rezultaty wykonania tasków)

PRIORYTETY ZADAN:

Nie znam rozkładu priorytetów przychodzących zadań. Ale pewnie będzie on inny niż sposób w jaki mam korzystać z serwisu.
Znaczy to, że niektórych zadań nie wyślę do serwisu, jeżeli chcę, żeby zadane średnie były zachowane.
Takich zadań będzie tym więcej im bardziej rozkład priorytetów przychodzących zadań będzie różny od sposobu korzystania z serwisu.

Co zrobić z takimi zadaniami?

Zakładam, że wolno mi je odrzucić. Mógłbym teoretycznie je zbierać i spróbować coś z nimi robić w przyszłości,
ale w praktyce ich liczba będzie tylko narastać.

Moim rozwiązaniem problemu priorytetów jest więc:
-Czekaj aż przyjdzie tyle zadań, że istnieje dla nich podzbiór który ma odpowiednie priorytety (w idealnym przypadku wystarczy 10 takich zadań )
-Prześlij ten podzbiór dalej
-Odrzuć pozostałe zadania

Fragment flow który to realizuje znajduje się w pliku ForceFrequency.scala


GLOBALNA PULA ZAPYTAN:

Zakładam że pula jest zużywana tylko w momencie gdy serwis zwróci OK ( Errory nie zużywają puli )
Tu po prostu czekam aż wpuszczę 100k zadań do flow, kolejne bedzie musiało poczekać do końca przedziału czasowego
(korzystam z DelayFlow z akka.stream.contrib + napisałem custom DelayStrategy ( DelayIfToManyInDuration.scala)
Dla uproszczenia nie obsługuje błędu 403 - nie powinien wystąpić w przypadku gdy z góry wiem czego mi nie wolno.
W testach oczywiście nie czekam miesiąc, tylko używam mniejszych wartości.


NIEZNANY RATE LIMITING:

W przypadku błędu 429 zadanie wraca do ponownej obsługi (wprowadziłem cykl w grafie obsługi).
Flow, gdy widzi zadanie które wraca z błędem 429, to na stałe zwiększa opóźnienie dla wszystkich zadań
W ten sposób po skończonej liczbie błędów 429, rate w końcu dostosuje sie do takiego które jest akceptowane przez serwis,
a żadne zadanie nie zostanie stracone.
(tu też korzystam z DelayFlow z akka.stream.contrib + custom DelayStrategy ( IncreaseDelayOnError.scala)

---------------------------------------------------------------------------------------------------------

Obsługa:

Projekt jest napisany w scali i zarządzany przez sbt.
Główny komponent znajduje się w pliku ExpensiveServiceFlow.scala
W pliku ExpensiveServiceFlowTest.scala znajdują się testy które sprawdzają 3 główne założenia/ograniczenia komponentu jako całości.






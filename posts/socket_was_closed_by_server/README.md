# Spring Batch 사용시 socket was closed by server 발생한다면



```java
Caused by: java.io.EOFException: unexpected end of stream, read 0 bytes from 4 (socket was closed by server)
```

![cause](./images/cause.png)

MySQL은 기본적으로 자신에게 맺어진 커넥션 중 일정 시간이상 사용하지 않은 커넥션을 종료하는 프로세스가 존재합니다.  
(```show global variables like 'wait_timeout';``` 으로 확인할 수 있고 default 8시간)

이를 위해 기존 커넥션풀은 대부분 연결을 맺은 커넥션들이 끊기는 것을 방지하기 위해 ```SELECT 1``` 등의 validation query를 주기적으로 날려 이 문제를 회피하는 반면 HikariCP는 maxLifetime 설정값에 따라 스스로 미사용된 커넥션을 제거하고 새로 생성하는 방식으로 동작한다고 합니다.

* HikariCP maxLifeTime: Connection Pool레벨에서 maxLifeTime이 지나도록 idle 상태인 connection 객체를 pool에서 제거합니다.
* MySQL wait_timeout: MySQL DBMS에서 wait_timeout 시간이 지나도록 사용하지 않은 Connection에 대해 Connection 연결을 해제합니다.
    * 사용하지 않았다는 의미는 Connection을 이용하여 어떠한 Query도 실행하지 않았음을 의미합니다.
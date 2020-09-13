# Spring Batch 사용시 socket was closed by server 발생한다면

AWS 이관 

```java
Caused by: java.io.EOFException: unexpected end of stream, read 0 bytes from 4 (socket was closed by server)
```

![cause](./images/cause.png)

MySQL은 기본적으로 자신에게 맺어진 커넥션 중 일정 시간이상 사용하지 않은 커넥션을 종료하는 프로세스가 존재합니다.  
(```show global variables like 'wait_timeout';``` 으로 확인할 수 있고 default 8시간)

이를 위해 기존 커넥션풀은 대부분 연결을 맺은 커넥션들이 끊기는 것을 방지하기 위해 ```SELECT 1``` 등의 validation query를 주기적으로 날려 이 문제를 회피하는 반면 HikariCP는 maxLifetime 설정값에 따라 스스로 미사용된 커넥션을 제거하고 새로 생성하는 방식으로 동작한다고 합니다.

* ```wait_timeout``` (MySQL)
  * MySQL 와 클라이언트 가 연결을 맺은 후, 다음 쿼리까지 기다리는
최대 시간을 의미합니다.
  * MySQL에 연결된 클라이언트 (여기서는 WAS등)가 지정된 wait_timeout 시간 동안 쿼리 요청이 없는 경우 MySQL은 해당 커넥션(connection) 을 강제로 종료해버립니다.
  * 기본값은 28800이며, 단위는 초(s) 라서 실제 기본 값은 8시간입니다.
* ```maxLifeTime``` (HikariCP)
  * 커넥션 풀에서 살아있을 수 있는 커넥션의 최대 수명시간. 
  * 사용중인 커넥션은 maxLifetime에 상관없이 제거되지않음. 
    * 사용중이지 않을 때만 제거됨.
    * 
  * 풀 전체가아닌 커넥션 별로 적용이되는데 그 이유는 풀에서 대량으로 커넥션들이 제거되는 것을 방지하기 위함
  * 기본값은 1800000이며, 단위는 초(ms) 라서 실제 기본 값은 30분입니다.
    * 0으로 지정하시면 무한대가 됩니다 (주의)
  * Connection Pool레벨에서 maxLifeTime이 지나도록 idle 상태인 connection 객체를 pool에서 제거합니다.
    * 사용하지 않았다는 의미는 Connection을 이용하여 어떠한 Query도 실행하지 않았음을 의미합니다.


아래와 같이 설정하면 HikariCP의 로그를 상세하게 볼 수 있습니다.

```yml
logging:
  level:
    com.zaxxer.hikari.HikariConfig: DEBUG
```

![hikaripool-log](./images/hikaripool-log.png)

> HouseKeeper란 HikariCP에서 사용하는 Connection Pool 관리 Thread입니다.


## Reader / Processor / Writer에서 DB를 사용하지 않을때

외부 API 연동이라던가, 혹은 트랜잭션 롤백이 보장되지 않아도 되는 경우에 위와 같은 예외가 발생할 수 있는데요.  
  
이를 해결할 수 있는 방법은 2가지가 있습니다.

* MySQL의 ```wait_timeout```와 HikariCP의 ```maxLifeTime``` 를 충분히 늘려놓기
  * 저 같은 경우 대량의 데이터를 처리하는 프로젝트의 경우
  * API 서버는 HikariCP의 ```maxLifeTime```를 58초로, Batch 서버는 HikariCP의 ```maxLifeTime```를 30분 (기본값)으로 맞춥니다.
  * 둘 다 같은 DB를 보고 있어 MySQL의 ```wait_timeout```를 32분으로 맞춥니다.
* ```ResourcelessTransactionManager``` 사용하기
  * 트랜잭션 롤백/커밋이 필요 없는 경우 굳이 Spring Batch의 트랜잭션 매니저가 필요로 하진 않습니다.
  * 그럴때를 대비해 
  * [참고 - KSUG 그룹](https://groups.google.com/g/ksug/c/jxcvvn1UXMk/m/EyBs83QhIr4J)
에는 굳이 Spring Batch의 트랜잭션 매니저를 사용하지 않아도 됩니다.





## Socket Close 테스트


* ```socketTimeout=120000``` (120초)
* ```maxLifetime: 58000``` (58초)
* ```wait_timeout: 60``` (60초)


```java
Communications link failure with primary host settler-beta.cluster-cdfmjscyqe71.ap-northeast-2.rds.amazonaws.com:6025. Connection timed out
```

## Socket Timeout 테스트

maxLifetime은 사용하지 않는 커넥션 한정으로 적용


## 참고

* [카카오커머스 기술 블로그 - JDBC 커넥션 풀들의 리소스 관리 방식 이해하기](https://kakaocommerce.tistory.com/45)
* [pkgonan - HikariCP는 test-while-idle과 같은 커넥션 갱신 기능이 없을까?](https://pkgonan.github.io/2018/04/HikariCP-test-while-idle)
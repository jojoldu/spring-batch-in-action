# Spring Batch 관리 도구로서의 CI 

Spring Batch는 아직까지 **표준 관리 도구**로 불리는 도구는 없습니다.  
각 팀/회사마다 상이한 방법들을 사용합니다.  
대표적인 방법들은 아래와 같습니다.

* Cron
* Spring MVC + API Call
* Spring Batch Admin
  * **Deprecated** 되었습니다.
    * 더이상 개선하지 않겠다고 합니다.
  * **Spring Cloud Data Flow** 로 전환하라고 합니다.
    * [참고](https://github.com/spring-attic/spring-batch-admin)
* Quartz + 직접 만든 Admin
  * Scheduler로 역할로 Quartz를 사용하고 그에 대한 UI 대시보드를 직접 만드는 경우입니다.
  * [참고](https://kingbbode.tistory.com/38)
* CI 서비스 (Jenkins / Teamcity 등등)

> Spring Cloud Data Flow는 아직 실무에서 써보질 않아서 비교대상에 넣질 못했습니다.  
> 하나씩 테스트 중인데 실제 실무에서도 사용해보고, 정리도 어느정도 되면 한번 블로그에 비교 & 공유하겠습니다.  

인터넷을 돌아다니다보면 아직까지 Spring Batch Admin 에 대해 언급되거나 Quartz를 사용한 어드민 페이지를 만들어 스프링 배치를 관리하는 글을 보게 됩니다.  
  
그래서 이번 글에서는 스프링 배치 관리도구로 CI 서비스들을 쓰면 어떤 장점이 있는지 소개하겠습니다.  

> ps. CI 도구로 스프링 배치를 사용하자는 일종의 약팔이로 보시면 됩니다.  
> ps. Spring Cloud Data Flow가 국내에서 좀 더 활성화가 많이 되는 시기가 온다면 그땐 이 글이 필요 없을 수도 있습니다.  
> 하지만 현재는 Spring Cloud Data Flow가 국내에 활성화가 안된 상태라 자료나 커뮤니티가 거의 없다고 보시면 됩니다.
  

## Jenkins?

Jenkins는 Java 진영의 대표적인 CI 툴입니다.  
대부분의 경우에 Jenkins는 배포 용도로 사용됩니다.  
그러다보니 배치에서도 Jenkins를 써야된다고 하면 거부감이 들기 마련입니다.  
그래서 저 같은 경우 Jenkins를 2대를 운영하여 각각이 개별 서버에서 격리되어 관리되도록 구성해서 사용하기도 합니다.  
(ex: 배포 젠킨스는 DB에 대한 권한을 제거하고, 배치 젠킨스는 배포 권한을 제거하는 등)

![deploy](./images/deploy.png)

(참고: [배포 Jenkins에서 배치 Jenkins로 Spring Batch 배포하기](https://jojoldu.tistory.com/313))


## 장점

* Integration
  * Slack
  * Email
  * Elastic Search Log 전송
* 기본적인 관리자 페이지 기능
  * 실행 이력
  * 로그 관리
  * Dashboard
* 다양한 실행 방법 (Rest API / 스케줄링 / 수동 실행)
* 계정별 권한 관리
* 파이프라인
* 다양한 파이프라인 관리 방법
  * Web UI
  * Script
  * 둘다 사용 가능
* 풍부한 Plugin 생태계
  * Ansible
  * Github 로그인
  * Logentries 등
* 추가 개발 공수가 필요하지 않다.


### 파이프라인

### 배치 애플리케이션 구조의 변화

위와 같은 CI 도구를 배치 관리자로 도입되면서 배치 애플리케이션 구현에도 많은 변화가 가능합니다.





## 단점

* 빈약한 검색 기능
* 필터 / 파라미터 노출 등이 없는 실행 이력 관리
* 신뢰할 수 없는 플러그인
* 파일로 관리 되는 메타 데이터
  * JetBrains사의 TeamCity의 경우 이런 메타 데이터들을 전부 Database로 관리합니다.
  * 

## 대안?


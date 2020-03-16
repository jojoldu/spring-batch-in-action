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
  * Scheduler 역할로 Quartz를 사용하고 그에 대한 UI 대시보드를 직접 만드는 경우입니다.
  * [참고](https://kingbbode.tistory.com/38)
* CI 서비스 (Jenkins / Teamcity 등등)

> Spring Cloud Data Flow는 아직 실무에서 써보질 않아서 비교대상에 넣질 못했습니다.  
> 하나씩 테스트 중인데 정리가 어느정도 되면 블로그에 공유하겠습니다.  

인터넷을 돌아다니다보면 아직까지 Spring Batch Admin 에 대해 언급되거나 직접 어드민 페이지를 만들어 스프링 배치를 관리하는 글을 보게 됩니다.  
  
그래서 이번 글에서는 스프링 배치 관리도구로 CI 서비스들을 쓰면 어떤 장점이 있는지 소개하겠습니다.  

> ps. Spring Cloud Data Flow가 국내에서 좀 더 활성화가 많이 되는 시기가 온다면 그땐 이 글이 필요 없을 수도 있습니다.  
> 하지만 현재는 Spring Cloud Data Flow가 국내에 활성화가 안된 상태라 자료나 커뮤니티가 거의 없다고 보시면 됩니다.
  

## 1. Jenkins?

Jenkins는 Java 진영의 대표적인 CI 툴로 대부분의 경우에 배포 용도로 사용됩니다.  
그러다보니 배치에서도 Jenkins를 써야된다고 하면 거부감이 들기 마련입니다.  
  
보통 그런 거부감은 **배포하는 서비스가 배치를 다룬다**라는 것 때문에 생기는데요.  
그래서 저 같은 경우 **Jenkins 2대**를 각각 개별 서버에서 격리되어 관리되도록 구성해서 사용하기도 합니다.  
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

* Job과 Step의 1:1 관계

* 기존에 Step으로 나눠진 작업들을 Job이 묶어주던 구조는

* Jenkins의 파이프라인이 각각의 Job을 묶어주는 구조로 변경


## 단점

* 소수의 배치를 관리하기 위해 사용하기엔 과함
  * 2~3개의 배치만 필요한 상황에서는 부담스러운 면이 있습니다.
* 빈약한 검색 기능
* 필터 / 파라미터 노출 등이 없는 실행 이력 관리
* 신뢰할 수 없는 플러그인
  * 대부분의 플러그인이 개인이 만든것들이다보니 **젠킨스 버전업을 못따라갑니다**
  * 못따라간 플러그인들은 젠킨스 버전업시 사용못하는 경우가 빈번합니다.

* 백업 & 이중화의 어려움
  * Jenkins는 Jenkins의 설정 정보를 비롯한, Job 실행 이력, Job 설정 정보등이 전부 **파일로 관리**됩니다.
  * 그러다보니 ```rsync``` 등으로 백업서버로 게속 
  * 비슷한 CI 도구인 Jetbrains사의 Teamcity의 경우 이런 정보들이 전부 **DB에서 관리**되다보니, 

## 대안?


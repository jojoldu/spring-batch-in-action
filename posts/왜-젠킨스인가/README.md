# 왜 Spring Batch 관리 Tool로서의 Jenkins

Spring Batch는 아직까지 **표준 관리 Tool**로 불리는 도구는 없습니다.  
각 팀/회사마다 상이한 방법들을 사용합니다.  
대표적인 방법들은 아래와 같습니다.

* Cron
* Spring MVC + API Call
* Spring Batch Admin (Deprecated)
  * Spring Cloud Data Flow
* Quartz + Admin
* CI Tools (Jenkins / Teamcity 등등)

> Spring Cloud Data Flow는 아직 실무에서 써보질 않아서 비교대상에 넣질 못했습니다.
> 하나씩 테스트 중인데 어느정도 정리가 되면 한번 블로그에 공유하겠습니다.

## Jenkins?

Jenkins는 Java 진영의 대표적인 CI 툴입니다.  
대부분 Jenkins는 배포 용도로 사용됩니다.  
그러다보니 배치에서도 Jenkins를 써야된다고 하면 거부감이 들기 마련입니다.  
  
실제로 이와 관련해서 친한 지인과 많은 이야기(를 가장한 온라인 배틀)을 했었는데요.  
주장은 이렇습니다.  
  



> 현재는 지인 역시 Jenkins (와 같은 CI 툴)로 Spring Batch를 관리하는게 **최고는 아니지만 최선**이라고 생각한다고 합니다.

## 장점

* Integration (Slack, Email 등)
* 실행 이력 / 로그 관리 / Dashboard
* 다양한 실행 방법 (Rest API / 스케줄링 / 수동 실행)
* 계정 별 권한 관리
* 파이프라인
* Web UI + Script 둘다 사용 가능
* Plugin (Ansible, Github, Logentries 등)
* 추가 개발 공수가 필요하지 않다.


## 단점

* 빈약한 검색 기능
* 필터 / 파라미터 노출 등이 없는 실행 이력 관리
* 신뢰할 수 없는 플러그인
* 파일로 관리 되는 메타 데이터

## 대안?


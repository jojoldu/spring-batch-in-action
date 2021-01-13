# Reader & Master 커넥션 분리되었을때 socket timeout 발생한 경우

* 전체 txManager는 마스터 커넥션
* ItemReader와 Processor 내부의 쿠폰 조회는 리더 커넥션
* Processor의 나머지는 마스터 커넥션

문제는

* 리더 커넥션을 사용하는 2번째에서 wait_timeout을 초과하게 되어 다음 마스터 커넥션 사용시 이미 DB에선 타임아웃 발새


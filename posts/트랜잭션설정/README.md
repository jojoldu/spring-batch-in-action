# Spring Batch에서 트랜잭션 설정하기

![atrribute](images/attribute.png)

* http://opennote46.tistory.com/82
  
## 트랜잭션 전파

NOT_SUPPORTED 새로운 트랜잭션이 생성된다. 새로운 트랜잭션이라면 Propagation.REQUIRES_NEW 와 동일한 기능이 아닌가? 하지만 약간 다르다. Propagation.REQUIRES_NEW는 새롭긴 하지만 부모(새로만들어지기전) 트랜잭션의 영향이 있다. 부모 트랜잭션이 에러가 발생하면 새로 만들어진 트랜잭션도 롤백이 된다.한마디로 기존 트랜잭션을 잠시 보류하고 새로운 트랜잭션을 진행한 후에 다시 기존 트랜잭션이 동작하게 된다. NOT_SUPPORTED 경우에는 별개다 만약 부모 트랜잭션에서 오류가 발생하여 롤백을 해도 새로운 트랜잭션은 롤백되지 않는다.

* http://wonwoo.ml/index.php/post/966


```java
	/**
	 * Create a new DefaultTransactionAttribute with the given
	 * propagation behavior. Can be modified through bean property setters.
	 * @param propagationBehavior one of the propagation constants in the
	 * TransactionDefinition interface
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 */
	public DefaultTransactionAttribute(int propagationBehavior) {
		super(propagationBehavior);
	}
```

```java
public interface TransactionDefinition {
    int PROPAGATION_REQUIRED = 0;
    int PROPAGATION_SUPPORTS = 1;
    int PROPAGATION_MANDATORY = 2;
    int PROPAGATION_REQUIRES_NEW = 3;
    int PROPAGATION_NOT_SUPPORTED = 4;
    int PROPAGATION_NEVER = 5;
    int PROPAGATION_NESTED = 6;

    int ISOLATION_DEFAULT = -1;

    int ISOLATION_READ_UNCOMMITTED = Connection.TRANSACTION_READ_UNCOMMITTED;

    int ISOLATION_READ_COMMITTED = Connection.TRANSACTION_READ_COMMITTED;

    int ISOLATION_REPEATABLE_READ = Connection.TRANSACTION_REPEATABLE_READ;

    int ISOLATION_SERIALIZABLE = Connection.TRANSACTION_SERIALIZABLE;


    int TIMEOUT_DEFAULT = -1;


    int getPropagationBehavior();

    int getIsolationLevel();

    int getTimeout();

    boolean isReadOnly();

   @Nullable
    String getName();
```
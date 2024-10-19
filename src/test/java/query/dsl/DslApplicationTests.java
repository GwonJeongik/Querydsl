package query.dsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import query.dsl.entity.QTestEntity;
import query.dsl.entity.TestEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DslApplicationTests {

    @Autowired
    private EntityManager em;

    @Test
    void contextLoads() {
        TestEntity testEntity = new TestEntity();
        em.persist(testEntity);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QTestEntity qTestEntity = QTestEntity.testEntity;

        TestEntity result = query.selectFrom(qTestEntity)
                .fetchOne();

        assertThat(result).isEqualTo(testEntity);
        assertThat(result.getId()).isEqualTo(testEntity.getId());
    }

}

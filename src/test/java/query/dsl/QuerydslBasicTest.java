package query.dsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import query.dsl.entity.Member;
import query.dsl.entity.QMember;
import query.dsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static query.dsl.entity.QMember.member;
import static query.dsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory query;

    @BeforeEach
    public void before() {
        query = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    @DisplayName(value = "JPQL로 특정 회원을 찾아라!")
    void startJpql() {
        //when
        Member findMember = em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", "member1")
                .getSingleResult();

        //then
        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    @DisplayName(value = "Querydsl로 특정 회원을 찾아라!")
    void startQuerydsl() {
        //when
        Member findMember = query
                .selectFrom(member)
                .where(member.name.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        //then
        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    @DisplayName(value = "Querydsl 검색 조건 설정!")
    void search() {
        //when
        Member findMember = query
                .selectFrom(member)
                .where(
                        member.name.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        //then
        assertThat(findMember.getName()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    void fetchResult() {
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

        Member fetchOne = query
                .selectFrom(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        Member fetchFirst = query
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> fetchResults = query
                .selectFrom(member)
                .fetchResults();

        long total = fetchResults.getTotal();

        List<Member> content = fetchResults.getResults();

        long totalCount = query
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 1. 나이로 내림차순 정렬
     * 2. 이름으로 올림차순 정렬
     * 3. 2 에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() {
        //given
        Member memberNull = new Member(null, 100);
        Member member5 = new Member("member5", 100);
        Member member6 = new Member("member6", 100);

        em.persist(memberNull);
        em.persist(member5);
        em.persist(member6);

        //when
        List<Member> result = query
                .selectFrom(member)
                .orderBy(
                        member.age.desc(),
                        member.name.asc().nullsLast()
                )
                .fetch();

        Member findMember5 = result.get(0);
        Member findMember6 = result.get(1);
        Member findMemberNull = result.get(2);

        //then
        assertThat(findMember5.getName()).isEqualTo("member5");
        assertThat(findMember6.getName()).isEqualTo("member6");
        assertThat(findMemberNull.getName()).isNull();
    }

    @Test
    void paging() {
        //when
        List<Member> result = query
                .selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetch();

        //then
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void pagingTotalCount() {
        //when
        Long totalCount = query
                .select(member.count())
                .from(member)
                .where(member.age.lt(30))
                .fetchOne();

        //then
        assertThat(totalCount).isEqualTo(2);
    }

    @Test
    @DisplayName(value = "함수")
    void aggregation() {
        //when
        List<Tuple> result = query
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        //then
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀 이름과 각 팀의 평균 연령을 구해라
     */
    @Test
    void groupBy() {
        //when
        List<Tuple> result = query
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * `teamA`에 소속된 모든 회원
     */
    @Test
    void join() {
        //when
        List<Member> result = query
                .select(member)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        //then
        assertThat(result)
                .extracting("name")
                .containsExactly("member1", "member2");
    }

    @Test
    @DisplayName("연관관계가 없는 필드로 조인")
    void theta_join() {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Member> result = query
                .select(member)
                .from(member, team)
                .where(member.name.eq(team.name))
                .fetch();

        //then
        assertThat(result)
                .extracting("name")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 `teamA`인 팀만 조인, 회원은 모두 조회
     * JPQL : select m,t from Member m left join m.team t on t.name = "teamA"
     */
    @Test
    void join_on() {
        //when
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL : select m from Member m leftJoin Team t on m.name = t.name
     */
    @Test
    @DisplayName("연관관계가 없는 엔티티로 조인")
    void join_on_no_relation() {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.name.eq(team.name))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetch_join_no() {
        //given
        System.out.println("========================");
        em.flush();
        em.clear();
        System.out.println("========================");

        //when
        Member findMember = query
                .selectFrom(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        //then
        assertThat(loaded).as("패치 조인 적용").isFalse();
    }

    @Test
    void fetch_join() {
        //given
        em.flush();
        em.clear();

        //when
        Member findMember = query
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.name.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember);

        //then
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }

    @Test
    void subQueryEq() {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    void subQueryGoe() {
        //given
        QMember memberSub = new QMember("subMember");

        //when
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 서브 쿼리 여러 건 처리, in 활용
     */
    @Test
    void subQueryIn() {
        //given
        QMember memberSub = new QMember("subMember");

        //when
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }


    /**
     *
     */
    @Test
    void subQueryToSelect() {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Tuple> result = query
                .select(member.name,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void simpleCase() {
        //when
        List<String> result = query
                .select(member.age
                        .when(10).then("열 살")
                        .when(20).then("스무 살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        //then
        assertThat(result).containsExactly("열 살", "스무 살", "기타", "기타");
    }

    @Test
    void complexCase() {
        //when
        List<String> result = query
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        //then
        assertThat(result).containsExactly("0~20살", "0~20살", "21~30살", "기타");
    }

    /**
     * 0 ~ 30살이 아닌 회원을 가장 먼저 출력
     * 0 ~ 20살 회원 출력
     * 21 ~ 30살 회원 출력
     */
    @Test
    void complexCaseUseOrderBy() {
        //given
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);
        //when
        List<Tuple> result = query
                .select(member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("age = " + tuple.get(member.age) + ", rank : " + tuple.get(rankPath));
        }
    }
}

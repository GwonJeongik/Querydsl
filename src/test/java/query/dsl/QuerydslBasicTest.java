package query.dsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import query.dsl.entity.Member;
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

}

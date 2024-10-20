package query.dsl.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "memberName", "age"})
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String name;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // == 생성자 == //
    public Member(String name, int age, Team team) {
        this.name = name;
        this.age = age;

        if (team != null) {
            changeTeam(team);
        }
    }

    public Member(String name, int age) {
        this(name, age, null);
    }

    public Member(String name) {
        this(name, 0);
    }

    /* 연관관계 편의 메서드 */
    private void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}

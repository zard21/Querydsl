package kr.co.bomapp.querydsl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.co.bomapp.querydsl.domain.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static kr.co.bomapp.querydsl.domain.user.QUser.*;
import static kr.co.bomapp.querydsl.domain.user.QDepartment.*;

@SpringBootTest
@Transactional
class ApplicationTests {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void preProcess() {
        queryFactory = new JPAQueryFactory(em);
        Department dept1 = new Department("dept1");
        Department dept2 = new Department("dept2");

        em.persist(dept1);
        em.persist(dept2);

        User user1 = new User("user1", 10, dept1);
        User user2 = new User("user2", 20, dept1);
        User user3 = new User("user3", 30, dept2);
        User user4 = new User("user4", 40, dept2);

        em.persist(user1);
        em.persist(user2);
        em.persist(user3);
        em.persist(user4);
    }

    @Test
    void contextLoads() {
    }

    @Test
    public void testQuerydsl() {
        QUser user = QUser.user;

        User queryUser = queryFactory
                .select(user)
                .from(user)
                .where(user.name.eq("user1"))
                .fetchOne();

        assertThat(queryUser.getName()).isEqualTo("user1");
    }

    // 기본 조회
    @Test
    public void query() {
        User queryUser = queryFactory
                .selectFrom(user)
                .where(user.name.eq("user1"), user.age.eq(10))
                .fetchOne();

        assertThat(queryUser.getName()).isEqualTo("user1");
    }

    // AND 조건 추가
    @Test
    public void queryWithAnd() {
        List<User> users = queryFactory
                .selectFrom(user)
                .where(user.name.eq("user1"), user.age.eq(10))
                .fetch();

        assertThat(users.size()).isEqualTo(1);
    }

    // 결과 형식
    @Test
    public void testResultType() {
        // List
        List<User> users = queryFactory
                .selectFrom(user)
                .fetch();

        // Single data
        User user1 = queryFactory
                .selectFrom(user)
                .fetchOne();

        // First single data
        User user2 = queryFactory
                .selectFrom(user)
                .fetchFirst();

        // Paging
        QueryResults<User> userResults = queryFactory
                .selectFrom(user)
                .fetchResults();

        // Count
        long count = queryFactory
                .selectFrom(user)
                .fetchCount();
    }

    // 정렬
    @Test
    public void testSorting() {
        List<User> users = queryFactory
                .selectFrom(user)
                .orderBy(user.age.desc(), user.name.asc().nullsLast())
                .fetch();

        for (User u : users) {
            System.out.println(u.getAge() + " / " + u.getName());
        }
    }

    // 페이징
    @Test
    public void testPaging() {
        List<User> users = queryFactory
                .selectFrom(user)
                .orderBy(user.name.desc())
                .offset(1)
                .limit(2)
                .fetch();

        for (User u : users) {
            System.out.println(u.getAge() + " / " + u.getName());
        }
    }

    @Test
    public void testPagingWithCount() {
        // Count를 위한 쿼리가 한 번 더 실행
        QueryResults<User> results = queryFactory
                .selectFrom(user)
                .orderBy(user.name.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        System.out.println(results.getTotal());
        System.out.println(results.getLimit());
        System.out.println(results.getOffset());
        System.out.println(results.getResults().size());
    }

    // 집합함수
    @Test
    public void testAggregation() {
        List<Tuple> result = queryFactory
                .select(user.count(),
                        user.age.sum(),
                        user.age.avg(),
                        user.age.max(),
                        user.age.min())
                .from(user)
                .fetch();

        Tuple tuple = result.get(0);
        System.out.println(tuple.get(user.count()));
        System.out.println(tuple.get(user.age.sum()));
        System.out.println(tuple.get(user.age.avg()));
        System.out.println(tuple.get(user.age.max()));
        System.out.println(tuple.get(user.age.min()));
    }

    // GroupBy
    @Test
    public void testGroupBy() {
        List<Tuple> result = queryFactory
                .select(department.name, user.age.avg())
                .from(user)
                .join(user.department, department)
                .groupBy(department.name)
                .fetch();

        Tuple dept1 = result.get(0);
        Tuple dept2 = result.get(1);

        System.out.println(dept1.get(department.name));
        System.out.println(dept1.get(user.age.avg()));

        System.out.println(dept2.get(department.name));
        System.out.println(dept2.get(user.age.avg()));
    }

    // Join
    // join(조인대상, 별칭으로 사용할 Q Type), innerJoin()
    // leftJoin(): left outer join
    // rightJoin(): right outer join
    @Test
    public void testJoin() {
        List<User> result = queryFactory
                .selectFrom(user)
                .join(user.department, department)
                .where(department.name.eq("dept1"))
                .fetch();

        assertThat(result)
                .extracting("name")
                .containsExactly("user1", "user2");
    }

    // 서브쿼리
    @Test
    public void testSubQueryEq() {
        QUser userSub = user;
        List<User> users = queryFactory
                .selectFrom(user)
                .where(user.age.eq(
                        JPAExpressions
                            .select(userSub.age.max())
                            .from(userSub)
                ))
                .fetch();

        assertThat(users).extracting("age").containsExactly(40);
    }

    @Test
    public void testSubQueryGoe() {
        QUser userSub = user;

        List<User> users = queryFactory
                .selectFrom(user)
                .where(user.age.goe(
                        JPAExpressions
                            .select(userSub.age.avg())
                            .from(userSub)
                ))
                .fetch();

        assertThat(users).extracting("age").containsExactly(30, 40);
    }

    @Test
    public void testSubQueryIn() {
        QUser userSub = user;

        List<User> users = queryFactory
                .selectFrom(user)
                .where(user.age.in(
                        JPAExpressions
                            .select(userSub.age)
                            .from(userSub)
                            .where(userSub.age.gt(10))
                ))
                .fetch();

        assertThat(users).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    public void testSelectSubQuery() {
        QUser userSub = user;

        List<Tuple> result = queryFactory
                .select(user.name,
                        JPAExpressions
                            .select(userSub.age.avg())
                            .from(userSub)
                ).from(user)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("name= " + tuple.get(user.name));
            System.out.println("age= " + tuple.get(JPAExpressions
                                                    .select(userSub.age.avg())
                                                    .from(userSub)));
        }
    }

    // Case
    @Test
    public void testCase() {
        List<String> result = queryFactory
                .select(user.age
                        .when(10).then("10years")
                        .when(20).then("20years")
                        .otherwise("Etc"))
                .from(user)
                .fetch();

        for (String age : result) {
            System.out.println(age);
        }
    }

    @Test
    public void testCase2() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(user.age.between(0, 20)).then("0-20")
                        .when(user.age.between(21, 30)).then("21-30")
                        .otherwise("Etc"))
                .from(user)
                .fetch();

        for (String age : result) {
            System.out.println(age);
        }
    }

    // Projection
    @Test
    public void testOneProjection() {
        List<String> result = queryFactory
                .select(user.name)
                .from(user)
                .fetch();

        for (String name : result) {
            System.out.println(name);
        }
    }

    @Test
    public void testMultiProjection() {
        List<Tuple> result = queryFactory
                .select(user.name, user.age)
                .from(user)
                .fetch();

        for (Tuple data : result) {
            String name = data.get(user.name);
            Integer age = data.get(user.age);

            System.out.println("name= " + name);
            System.out.println("age= " + age);
        }
    }

    @Test
    public void testQueryProjection() {
        List<UserDTO> result = queryFactory
                .select(new QUserDTO(user.name, user.age))
                .from(user)
                .fetch();

        for (UserDTO user : result) {
            System.out.println("name= " + user.getName());
            System.out.println("age= " + user.getAge());
        }
    }

    // 동적 쿼리
    @Test
    public void testWhereParam() {
        List<User> result = queryFactory
                .selectFrom(user)
                .where(nameEq(null), ageEq(10))
                .fetch();

        for (User user : result) {
            System.out.println("name= " + user.getName());
            System.out.println("age= " + user.getAge());
        }
    }

    private BooleanExpression nameEq(String nameParam) {
        return nameParam != null ? user.name.eq(nameParam) : null;
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam != null ? user.age.eq(ageParam) : null;
    }
}

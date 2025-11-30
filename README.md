# 🗓️ ToDoLab — 일정 관리 프로젝트

ToDoLab은 **일정 관리 기능을 베이스로**,  
제가 **공부하고 싶었던 기술들을 하나씩 붙여가며 확장해 나가는 실험형 프로젝트**입니다.

---

## 🧱 기술 스택

### Backend
- Java 21
- Spring Boot 3.4
- Spring WebFlux
- Spring Validation
- Project Reactor
- Spring Data JPA

### Frontend
- Thymeleaf

### Database
- MySQL 8.x

---

## 💡 기술 선택 이유

### Spring WebFlux vs Spring MVC
**비교군:** Spring MVC
- MVC는 쓰레드 기반 동기 모델로 구조는 단순하지만 고부하 환경에서 비효율적
- WebFlux는 논블로킹 기반으로 높은 동시성 처리에 유리함

**선택 이유:**  
리액티브 기반 고성능 구조를 직접 실험해보고 싶었고,  
Reactor 기반 데이터 흐름을 자연스럽게 다뤄보는 데 WebFlux가 더 적합하다고 판단했습니다.

---

### Thymeleaf vs React/Vue
**비교군:** React / Vue
- SPA는 확장성과 자유도가 높지만 초기 세팅이 무겁고 번거로움
- 이번 프로젝트의 목적은 복잡한 UI가 아니라 빠른 프로토타이핑

**선택 이유:**  
서버 사이드 렌더링 기반인 Thymeleaf로  
간단한 UI를 빠르게 구성하고, 핵심인 백엔드 구조에 집중하기 위해 선택했습니다.

---

### 🔄 R2DBC → JDBC 전환

초기에는 **리액티브 전체 흐름을 구성해보는 목적**으로 R2DBC를 선택했습니다.  
하지만 프로젝트가 확장되면서 현실적인 문제들이 나타났습니다.

### ❌ R2DBC 유지가 어려웠던 이유
1. **생태계 부족**
    - JPA가 제공하는 연관관계, 영속성 컨텍스트, Auditing 기능을 사용할 수 없음
    - QueryDSL, EntityGraph 등 고급 기능 미지원

2. **테스트 환경 불편**
    - R2DBC는 H2 Memory 테스트가 사실상 불가
    - Testcontainers 의존 → 테스트 실행 속도 저하

3. **WebFlux + JDBC 조합도 실무에서 많이 사용됨**
    - Spring 공식 문서에서도 허용된 구조
    - DB I/O는 별도 Scheduler로 분리하면 안정적

4. **프로젝트 목적과의 괴리**
    - 목적은 일정 관리 기능 + 구조 설계
    - DB 계층 실험이 발목을 잡으며 생산성 저하

---

### ✔ 최종 결정: JDBC + JPA

### JDBC + JPA를 선택한 이유
- JPA 기능(매핑, Auditing, 변경 감지 등)을 모두 활용 가능
- `@DataJpaTest` 를 통한 **빠르고 간편한 테스트 환경** 구축
- H2 Memory 기반 테스트로 개발 생산성 향상
- QueryDSL, JPQL 등 다양한 확장 기능 사용 가능
- WebFlux는 그대로 유지하여 서버는 논블로킹 구조를 유지
- **개발 효율성과 유지보수성이 전체적으로 개선됨**

---

### 🗄️ MySQL 선택 이유 (JDBC 환경 기준)

| DB | 안정성(JDBC) | 성능 | 사용성 | 비고 |
|----|--------------|------|--------|------|
| PostgreSQL | 매우 안정적 | 우수 | 보통 | 기능 강하지만 초기 설정 부담 존재 |
| MariaDB | 보통 | 좋음 | MySQL과 유사 | 사소한 문법 차이 존재 |
| Oracle | 매우 안정적 | 최고 | 낮음 | 개인 프로젝트에는 과한 선택 |
| **MySQL** | 안정적 ✔ | 빠름 ✔ | 매우 쉬움 ✔ | 개발 생산성 최적 |

**최종 선택 — MySQL**
- 가장 익숙하고 오류 해결이 빠름
- JPA + MySQL 조합은 실무 표준
- 무료 + 설정 간단

---

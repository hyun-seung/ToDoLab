# 🗓️ ToDoLab — 일정 관리 프로젝트

ToDoLab은 **일정 관리 기능을 베이스로**,  
제가 **공부하고 싶었던 기술들을 하나씩 붙여가며 확장해 나가는 실험형 프로젝트**입니다.

---

## 🧱 기술 스택

### Backend
- ~~Java 21~~ → **Java 25**
- ~~Spring Boot 3.4~~ → **Spring Boot 4.0.1**
- ~~Spring WebFlux~~ → **Spring MVC (Virtual Thread 기반)**
- Spring Validation
- ~~Project Reactor~~ → *(Virtual Thread 도입으로 의존성 제거)*
- Spring Data JPA

### Frontend
- Thymeleaf

### Database
- MySQL 8.x 

---

## 💡 기술 선택 이유

### Spring WebFlux vs Spring MVC

**비교군:** Spring MVC

- ~~MVC는 쓰레드 기반 동기 모델로 구조는 단순하지만 고부하 환경에서 비효율적~~  
  → **Virtual Thread 도입으로 Thread-per-request 모델의 확장성 한계가 크게 완화됨**

- ~~WebFlux는 논블로킹 기반으로 높은 동시성 처리에 유리함~~  
  → **논블로킹의 이점은 여전히 존재하지만, 단순 CRUD 도메인에서는 구조 복잡도가 더 큼**

**선택 이력:**
- ~~리액티브 기반 고성능 구조를 직접 실험해보고 싶었고, Reactor 기반 데이터 흐름을 다뤄보기 위해 WebFlux 선택~~
- **Java 21+ 이후 Virtual Thread 등장으로, 기존 MVC 모델을 유지하면서도 고동시성 처리가 가능해짐**
- **코드 가독성, 디버깅, 트랜잭션 처리 측면에서 MVC + Virtual Thread가 더 합리적이라고 판단**

---

### Virtual Thread 도입으로 바뀐 전제

- ~~논블로킹이 아니면 고동시성 처리가 어렵다~~  
  → **블로킹 코드도 대량 동시성 처리 가능**

- ~~WebFlux가 고성능 서버의 기본 선택~~  
  → **MVC + Virtual Thread가 새로운 범용 선택지로 부상**

- ~~Reactor 기반 흐름 제어가 필수~~  
  → **명령형 코드 기반 구조로 단순화 가능**

---

### WebFlux vs Virtual Thread 기반 MVC

| 항목 | ~~WebFlux~~ | Virtual Thread + MVC |
|----|------------|---------------------|
| 동시성 모델 | 논블로킹 | 경량 블로킹 |
| 코드 복잡도 | 높음 | 낮음 |
| 디버깅 | 어려움 | 쉬움 |
| JPA 궁합 | 제한적 | 매우 좋음 |

---

### 🔄 R2DBC → JDBC 전환

초기에는 **리액티브 전체 흐름을 구성해보는 목적**으로 R2DBC를 선택했습니다.  
하지만 프로젝트가 확장되면서 현실적인 문제들이 나타났습니다.

### ❌ R2DBC 유지가 어려웠던 이유
1. **생태계 부족**
   - JPA 연관관계, 영속성 컨텍스트, Auditing 미지원
2. **테스트 환경 불편**
   - H2 Memory 테스트 사실상 불가
   - Testcontainers 의존으로 테스트 속도 저하
3. **실무 구조와의 괴리**
   - 리액티브 DB 계층 실험이 프로젝트 본질을 흐림

---

### Thymeleaf vs React / Vue

**비교군:** React / Vue

- SPA는 확장성과 자유도가 높지만 초기 세팅이 무겁고 번거로움 *(기존 판단 유지)*
- 이번 프로젝트의 목적은 복잡한 UI가 아니라 **백엔드 구조 실험**

**선택 이유:**
- 서버 사이드 렌더링 기반으로 빠른 프로토타이핑 가능
- 프론트엔드 복잡도를 최소화하고 서버 구조에 집중하기 위함

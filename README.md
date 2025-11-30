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
- R2DBC

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

### R2DBC vs JDBC
**비교군:** JDBC
- JDBC는 Blocking I/O라 WebFlux와 잘 맞지 않음
- 반면 안정성과 생태계는 JDBC가 훨씬 우수함

**선택 이유:**  
실무에서는 JDBC를 선호하지만, 이번 프로젝트에서는  
**리액티브 전체 흐름을 처음부터 끝까지 적용해보고 싶은 목적**이 더 컸기 때문에 R2DBC를 선택했습니다.

---

### Thymeleaf vs React/Vue
**비교군:** React / Vue
- SPA는 확장성과 자유도가 높지만 초기 세팅이 무겁고 번거로움
- 이번 프로젝트의 목적은 복잡한 UI가 아니라 빠른 프로토타이핑

**선택 이유:**  
서버 사이드 렌더링 기반인 Thymeleaf로  
간단한 UI를 빠르게 구성하고, 핵심인 백엔드 구조에 집중하기 위해 선택했습니다.

---

### MySQL 선택 이유 (R2DBC + 비용 + 사용성 중심)
| DB | R2DBC 지원 | 비용 | 코멘트 |
|----|------------|------|---------|
| PostgreSQL | ✔️ | 무료 | 성능 좋고 기능 강함, 하지만 초기 설정이 다소 무거움 |
| MariaDB | ✔️ | 무료 | MySQL과 유사하지만 R2DBC 안정성에서 조금 아쉬움 |
| SQL Server | ✔️(커뮤니티) | 유료 | 학습 목적에는 과한 선택 |
| Oracle | ❌ | 유료 | R2DBC 미지원 |
| **MySQL** | ✔️(커뮤니티) | 무료 | 가장 익숙하고 설정이 빠름 |

**최종 선택 — MySQL**
- 개발 속도가 가장 빠름
- 익숙한 생태계로 트러블슈팅 부담 감소
- R2DBC도 무리 없이 연동 가능
- 무료라 개인 실험 프로젝트에 적합

**즉, 리액티브 실습 흐름을 끊지 않으면서도 가장 가볍게 적용 가능한 DB라는 기준에서 MySQL을 선택했습니다.**

---

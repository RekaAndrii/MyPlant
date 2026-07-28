---
name: myplant-coverage
description: "Use this skill when the user wants to run code coverage, check test coverage, view coverage report, see which classes are untested, improve coverage, add coverage tool, or verify overall test coverage for the MyPlant project. Trigger phrases: run coverage, check coverage, code coverage, test coverage, coverage report, what is coverage, show coverage, jacoco, untested classes, improve coverage, add coverage."
---

# MyPlant Code Coverage Skill

## Overview

MyPlant uses **JaCoCo 0.8.12** for code coverage, configured as a Maven plugin in `pom.xml`.

- **Tool:** JaCoCo (`jacoco-maven-plugin` 0.8.12)
- **Report format:** XML
- **Report location:** `target/site/jacoco/jacoco.xml`
- **Triggers:** automatically on every `./mvnw test` run — no separate command needed
- **Threshold:** none configured (build never fails due to coverage)

## Running Coverage

```bash
./mvnw test
```

The JaCoCo agent instruments classes before tests run and generates `target/site/jacoco/jacoco.xml` after the test phase completes.

## Checking Overall Coverage

Parse the report-level counters from `jacoco.xml` using Node.js:

```js
const fs = require('fs');
const xml = fs.readFileSync('target/site/jacoco/jacoco.xml', 'utf8');
const withoutPackages = xml.replace(/<package[\s\S]*?<\/package>/g, '');
const reportSection = withoutPackages.match(/<report[^>]*>([\s\S]*?)<\/report>/);
const order = ['INSTRUCTION','BRANCH','LINE','METHOD','CLASS'];
for (const [,type,missed,covered] of reportSection[1].matchAll(/<counter type="(\w+)" missed="(\d+)" covered="(\d+)"\/>/g)) {
  const total = parseInt(missed)+parseInt(covered), cov = parseInt(covered);
  const pct = total > 0 ? (cov/total*100).toFixed(1) : '0.0';
  if (order.includes(type)) console.log(type.padEnd(12)+pct.padStart(6)+'%   ('+cov+'/'+total+')');
}
```

## Checking Coverage by Package

```js
const fs = require('fs');
const xml = fs.readFileSync('target/site/jacoco/jacoco.xml', 'utf8');
const pkgs = {};
for (const m of xml.matchAll(/<class name="([^"]*?)" sourcefilename="([^"]*?)">([\s\S]*?)<\/class>/g)) {
  const pkg = m[1].substring(0, m[1].lastIndexOf('/')).replace(/\//g,'.');
  const lm = m[3].match(/<counter type="LINE" missed="(\d+)" covered="(\d+)"\/>/);
  if (lm) {
    if (!pkgs[pkg]) pkgs[pkg] = { missed:0, covered:0 };
    pkgs[pkg].missed += parseInt(lm[1]);
    pkgs[pkg].covered += parseInt(lm[2]);
  }
}
Object.entries(pkgs).sort((a,b)=>{
  const pa=(a[1].covered/(a[1].missed+a[1].covered)||0);
  const pb=(b[1].covered/(b[1].missed+b[1].covered)||0);
  return pb-pa;
}).forEach(([pkg,d])=>{
  const total=d.missed+d.covered;
  const pct=total>0?(d.covered/total*100).toFixed(1):'0.0';
  console.log(pkg.padEnd(50)+pct.padStart(6)+'%   ('+d.covered+'/'+total+' lines)');
});
```

## Checking Coverage for a Specific Area

Filter classes by keyword:

```js
const fs = require('fs');
const xml = fs.readFileSync('target/site/jacoco/jacoco.xml', 'utf8');
const keyword = 'auth'; // change to filter by package or class name
for (const m of xml.matchAll(/<class name="([^"]*?)" sourcefilename="([^"]*?)">([\s\S]*?)<\/class>/g)) {
  if (!m[1].toLowerCase().includes(keyword)) continue;
  const lm = m[3].match(/<counter type="LINE" missed="(\d+)" covered="(\d+)"\/>/);
  const bm = m[3].match(/<counter type="BRANCH" missed="(\d+)" covered="(\d+)"\/>/);
  if (lm) {
    const t=parseInt(lm[1])+parseInt(lm[2]);
    const pct=t>0?(parseInt(lm[2])/t*100).toFixed(1):'0.0';
    const bPct=bm?((parseInt(bm[2])/(parseInt(bm[1])+parseInt(bm[2]))*100).toFixed(1)+'%'):'n/a';
    console.log(m[2].padEnd(45)+'line: '+pct+'%   branch: '+bPct+'   ('+lm[2]+'/'+t+')');
  }
}
```

## Project Structure — Test Files

```
src/test/java/com/my/plant/
├── HexEncodingTest.java
├── MyPlantApplicationTests.java
├── controller/
│   ├── LoginControllerTest.java
│   ├── QuickControllerTest.java
│   ├── SuggestionControllerTest.java
│   └── UserControllerTest.java
└── service/
    └── impl/
        ├── CompositeUserDetailsServiceTest.java
        ├── SuggestionServiceImplTest.java
        └── UserServiceImplTest.java
```

## Current Baseline Coverage (as of last run)

| Metric | Coverage |
|---|---|
| Instruction | ~18% |
| Branch | ~19% |
| Line | ~20% |
| Method | ~19% |
| Class | ~29% |

### Auth coverage (fully tested)

| Class | Line % | Branch % |
|---|---|---|
| `CompositeUserDetailsService` | 100% | 100% |
| `UserServiceImpl` | 100% | 100% |
| `LoginController` | 100% | — |
| `UserController` | 100% | 100% |
| `QuickController` | 100% | — |
| `User` (model) | 100% | — |

### Known untested areas

| Package | Reason |
|---|---|
| `controller/` — `BlockController`, `HomeController` | Core app logic, no tests yet |
| `configs/WebSecurityConfig` | Spring Security config — not unit-testable |
| `util/UserUtil` | Static Spring Security call — tested indirectly |
| `util/ColorUtil`, `util/comparator/` | Utility helpers — no tests yet |
| `model/` — `Block`, `HistoryItem`, `Achievement` | Model getters/setters — low priority |

## Adding New Tests

Follow the existing test patterns:

- **Unit test style:** `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
- **Static mocks:** `MockedStatic<UserUtil>` via `mockStatic(UserUtil.class)` for `UserUtil.getLogginedUserName()`
- **Test file location:** mirror the main source tree under `src/test/java/com/my/plant/`
- **Naming:** `<ClassName>Test.java`, method names follow `methodName_expectedBehaviour_whenCondition`

## pom.xml Configuration Reference

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
            <configuration>
                <formats>
                    <format>XML</format>
                </formats>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Output Format

When reporting coverage results, use this format:

```
## Coverage Report — MyPlant

### Overall
| Metric      | Coverage |
|-------------|----------|
| Line        | XX.X%    |
| Branch      | XX.X%    |
| Instruction | XX.X%    |
| Method      | XX.X%    |
| Class       | XX.X%    |

### By Package (Line %)
| Package                  | Line % | Lines  |
|--------------------------|--------|--------|
| com.my.plant.service.impl| XX.X%  | X/X    |
...

### Key gaps
- List classes with 0% coverage that are worth testing
```

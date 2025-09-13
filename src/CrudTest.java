import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * 아주 단순한 콘솔 버전 (단일 파일)
 * - Load / Make / Find / Save / Print / Insert / Update / Delete / Exit
 * - 정규식 지원 X (그냥 '문자 그대로' 검색/치환)
 * - 전체 적용 or 선택 적용(인덱스 입력)만 제공
 * - 클래스패스 리소스 읽기 예시 포함 (sample.txt)
 */

public class CrudTest {
    private static String content = ""; // 로드한 파일 content에 저장
    private static String currentPath = null; // 파일 경로 저장 (절대/상대)

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.println("=== Simple Text CRUD (Console) ===");

        while (true) {
            showMenu();
            System.out.print("Select (1-9): ");
            String sel = sc.nextLine().trim();
            try {
                switch (sel) {
                    case "1": menuLoad(sc); break;
                    case "2": menuMake(sc); break;
                    case "3": menuFind(sc); break;
                    case "4": menuSave(sc); break;
                    case "5": menuPrint(); break;
                    case "6": menuInsert(sc); break;
                    case "7": menuUpdate(sc); break;
                    case "8": menuDelete(sc); break;
                    case "9": System.out.println("Bye"); return;
                    default: System.out.println("Invalid.");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }
    private static void showMenu() {
        System.out.println();
        System.out.println("1. Load");
        System.out.println("2. Make");
        System.out.println("3. Find");
        System.out.println("4. Save");
        System.out.println("5. Print");
        System.out.println("6. Insert");
        System.out.println("7. Update");
        System.out.println("8. Delete");
        System.out.println("9. Exit");
    }
    private static void menuLoad(Scanner sc) throws IOException {
        InputStream in = CrudTest.class.getClassLoader().getResourceAsStream("korean_netflix.txt");
        if (in == null) {
            System.out.println("리소스를 찾을 수 없습니다.");
            return;
        }
        content = readAll(in);
        currentPath = null; // 리소스는 경로 미저장
        System.out.println("리소스 로드 완료.");

    }

    private static void menuMake(Scanner sc) {
        System.out.println("1) 빈 문서  2) 직접 입력");
        System.out.print("선택: ");
        String s = sc.nextLine().trim();
        content = "";
        currentPath = null;
        if ("2".equals(s)) {
            System.out.println("한 줄씩 입력하세요. 종료하려면 :finish 만 입력");
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = sc.nextLine();
                if (":finish".equals(line)) break;
                sb.append(line).append("\n");
            }
            content = sb.toString();
        }
        System.out.println("새 문서 생성 완료.");
    }

    private static void menuFind(Scanner sc) {
        System.out.print("찾을 단어/문장: ");
        String q = sc.nextLine().trim();
        if (q.isEmpty()) { System.out.println("검색어가 비어있습니다."); return; }

        List<int[]> occs = findAll(content, q);
        if (occs.isEmpty()) { System.out.println("일치 항목 없음"); return; }
        System.out.println("발견: " + occs.size() + "개");
        for (int i = 0; i < occs.size(); i++) {
            int s = occs.get(i)[0], e = occs.get(i)[1];
            String ctx = context(content, s, e, 20);
            System.out.printf("[%d] ...%s...%n", i, ctx);
        }

    }
    private static void menuSave(Scanner sc) throws IOException {
        System.out.print("저장 경로(비우면 현재 경로로 저장): ");
        String p = sc.nextLine().trim();
        if (p.isEmpty()) {
            if (currentPath == null) {
                // C:\Users\byeon\OneDrive\바탕 화면\XML\memo1.txt
                System.out.println("현재 경로가 없습니다. 새 경로를 입력하세요.");
                return;
            } else {
                Files.writeString(Paths.get(currentPath), content, StandardCharsets.UTF_8);
                System.out.println("저장 완료: " + currentPath);
            }
        } else {
            // 자바 11부터 쓸 수 있는 간단한 파일 쓰기 API
            Files.writeString(Paths.get(p), content, StandardCharsets.UTF_8);
            currentPath = p;
            System.out.println("저장 완료: " + currentPath);
        }
    }
    private static void menuPrint() {
        if (content.isEmpty()) {
            System.out.println("빈 문서");
            return;
        }
        String[] lines = content.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            System.out.printf("%s%n", lines[i]);
        }
    }
    private static void menuInsert(Scanner sc) {
        if (content.isEmpty()) { System.out.println("(빈 문서) 먼저 Load 또는 Make 하세요."); return; }

        // 1) 어디에 삽입할지: 기준 문자열(앵커)과 대소문자 옵션
        System.out.print("어디에 삽입할지 기준이 될 단어/문장(문자 그대로): ");
        String anchor = sc.nextLine().trim();
        if (anchor.isEmpty()) { System.out.println("대상 문자열이 비어있습니다."); return; }

        // 2) 일치 위치 찾기 + 미리보기
        List<int[]> occs = findAll(content, anchor);
        if (occs.isEmpty()) { System.out.println("일치 항목 없음"); return; }

        System.out.println("발견: " + occs.size() + "개");
        for (int i = 0; i < occs.size(); i++) {
            int s = occs.get(i)[0], e = occs.get(i)[1];
            System.out.printf("[%d] ...%s...%n", i, context(content, s, e, 20));
        }

        // 3) 삽입 방향 선택: BEFORE or AFTER
        boolean before = askYesNo(sc, "기준 앞에 삽입할까요?(y=앞/ n=뒤): ");

        // 4) 적용 위치(인덱스) 선택: 전체/선택
        boolean all = askYesNo(sc, "모두 삽입할까요? (y/n): ");
        Set<Integer> indices = all ? null : readIndexSet(sc, occs.size(), "삽입할 인덱스(예: 0,2,5-8): ");

        // 5) 삽입할 문장 입력
        System.out.print("삽입할 문장(텍스트): ");
        String insertText = sc.nextLine();

        // 6) 재조립해 실제 삽입 수행
        StringBuilder sb = new StringBuilder();
        int last = 0, count = 0;

        for (int i = 0; i < occs.size(); i++) {
            int s = occs.get(i)[0], e = occs.get(i)[1];

            // 선택된 인덱스만 수행(전체면 null)
            if (indices == null || indices.contains(i)) {
                if (before) {
                    // [last .. s) + insert + [s .. e)
                    sb.append(content, last, s);
                    sb.append(insertText);
                    sb.append(content, s, e);
                    last = e;
                } else {
                    // [last .. e) + insert
                    sb.append(content, last, e);
                    sb.append(insertText);
                    last = e;
                }
                count++;
            }
        }
        sb.append(content.substring(last));
        if (count > 0) content = sb.toString();

        System.out.println("삽입 적용: " + count + "개");
    }

    private static void menuUpdate(Scanner sc) {
        if (content.isEmpty()) { System.out.println("(빈 문서) 먼저 Load 또는 Make 하세요."); return; }

        System.out.print("수정(치환)할 대상(문자 그대로): ");
        String q = sc.nextLine().trim();
        if (q.isEmpty()) { System.out.println("대상 문자열이 비어있습니다."); return; }

        System.out.print("이걸로 바꾸기(치환 문자열): ");
        String rep = sc.nextLine(); // 빈 문자열도 허용(=삭제처럼 동작 가능)

        List<int[]> occs = findAll(content, q);
        if (occs.isEmpty()) { System.out.println("일치 항목 없음"); return; }

        // 맥락 출력
        System.out.println("발견: " + occs.size() + "개");
        for (int i = 0; i < occs.size(); i++) {
            int s = occs.get(i)[0], e = occs.get(i)[1];
            System.out.printf("[%d] ...%s...%n", i, context(content, s, e, 20));
        }

        boolean all = askYesNo(sc, "모두 수정(치환)할까요? (y/n): ");
        Set<Integer> indices = all ? null : readIndexSet(sc, occs.size(), "적용할 인덱스(예: 0,2,5-8): ");

        // 재조립
        StringBuilder sb = new StringBuilder();
        int last = 0, count = 0;
        for (int i = 0; i < occs.size(); i++) {
            int s = occs.get(i)[0], e = occs.get(i)[1];
            if (indices == null || indices.contains(i)) {
                sb.append(content, last, s);
                sb.append(rep);        // 치환
                last = e;
                count++;
            }
        }
        sb.append(content.substring(last));
        if (count > 0) content = sb.toString();

        System.out.println("수정(치환) 적용: " + count + "개");
    }
    private static Set<Integer> readIndexSet(Scanner sc, int totalSize, String prompt) {
        System.out.print(prompt);
        String line = sc.nextLine().trim();
        Set<Integer> set = new LinkedHashSet<>();
        if (line.isEmpty()) return set; // 비우면 '아무 것도 선택 안 함'으로 처리

        for (String token : line.split(",")) {
            token = token.trim();
            if (token.isEmpty()) continue;
            if (token.contains("-")) {
                String[] parts = token.split("-");
                try {
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    if (a > b) { int t = a; a = b; b = t; }
                    for (int i = a; i <= b; i++) if (0 <= i && i < totalSize) set.add(i);
                } catch (Exception ignored) {}
            } else {
                try {
                    int v = Integer.parseInt(token);
                    if (0 <= v && v < totalSize) set.add(v);
                } catch (Exception ignored) {}
            }
        }
        return set;
    }
    private static void menuDelete(Scanner sc) {
        if (content.isEmpty()) { System.out.println("(빈 문서) 먼저 Load 또는 Make 하세요."); return; }

        System.out.print("삭제할 대상(문자): ");
        String q = sc.nextLine().trim();
        if (q.isEmpty()) { System.out.println("대상 문자열이 비어있습니다."); return; }

        List<int[]> occs = findAll(content, q);
        if (occs.isEmpty()) { System.out.println("일치 항목 없음"); return; }

        // 맥락 출력
        System.out.println("발견: " + occs.size() + "개");
        for (int i = 0; i < occs.size(); i++) {
            int s = occs.get(i)[0], e = occs.get(i)[1];
            System.out.printf("[%d] ...%s...%n", i, context(content, s, e, 20));
        }

        boolean all = askYesNo(sc, "모두 삭제할까요? (y/n): ");
        Set<Integer> indices = all ? null : readIndexSet(sc, occs.size(), "삭제할 인덱스(예: 0,2,5-8): ");

        // 재조립
        StringBuilder sb = new StringBuilder();
        int last = 0, count = 0;
        for (int i = 0; i < occs.size(); i++) {
            int s = occs.get(i)[0], e = occs.get(i)[1];
            if (indices == null || indices.contains(i)) {
                sb.append(content, last, s); // 대상 구간은 건너뛰어 삭제 효과
                last = e;
                count++;
            }
        }
        sb.append(content.substring(last));
        if (count > 0) content = sb.toString();

        System.out.println("삭제 적용: " + count + "개");
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toString(StandardCharsets.UTF_8);
    }

    private static String context(String text, int s, int e, int r) {
        int a = Math.max(0, s - r), b = Math.min(text.length(), e + r);
        String mid = text.substring(s, e);
        String left = text.substring(a, s).replace("\n", "\\n");
        String right = text.substring(e, b).replace("\n", "\\n");
        return left + "[" + mid + "]" + right;
    }
    private static List<int[]> findAll(String src, String needle) {
        List<int[]> occs = new ArrayList<>();
        if (needle.isEmpty()) return occs;
        int from = 0;
        while (true) {
            int idx = src.indexOf(needle, from);
            if (idx < 0) break;
            occs.add(new int[]{idx, idx + needle.length()});
            from = idx + needle.length();
        }
        return occs;
    }
    private static boolean askYesNo(Scanner sc, String string) {
        System.out.print(string);
        String s = sc.nextLine().trim().toLowerCase();
        return s.startsWith("y");
    }

}
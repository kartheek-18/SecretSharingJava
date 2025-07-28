import java.math.BigInteger;
import java.util.*;

class SecretSharingSolver {

    static class Point {
        int x;
        BigInteger y;
        Point(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    static BigInteger decodeValue(String val, int base) {
        return new BigInteger(val, base);
    }

    // Encode BigInteger back to string in given base
    static String encodeValue(BigInteger val, int base) {
        return val.toString(base);
    }

    static BigInteger[] solvePolynomialCoefficients(int degree, int k, List<Point> points) {
        BigInteger[][] A = new BigInteger[k][k];
        BigInteger[] Y = new BigInteger[k];

        for (int i = 0; i < k; i++) {
            int x = points.get(i).x;
            BigInteger y = points.get(i).y;
            Y[i] = y;
            for (int j = 0; j < k; j++) {
                int power = degree - j;
                A[i][j] = BigInteger.valueOf(x).pow(power);
            }
        }

        return gaussianElimination(A, Y);
    }

    static BigInteger[] gaussianElimination(BigInteger[][] A, BigInteger[] Y) {
        int n = A.length;
        BigInteger[][] M = new BigInteger[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            M[i][n] = Y[i];
        }

        for (int i = 0; i < n; i++) {
            int maxRow = i;
            for (int r = i + 1; r < n; r++) {
                if (M[r][i].abs().compareTo(M[maxRow][i].abs()) > 0) {
                    maxRow = r;
                }
            }
            if (maxRow != i) {
                BigInteger[] temp = M[i];
                M[i] = M[maxRow];
                M[maxRow] = temp;
            }
            if (M[i][i].equals(BigInteger.ZERO)) {
                throw new ArithmeticException("Singular matrix - cannot solve");
            }
            BigInteger pivot = M[i][i];
            for (int col = i; col <= n; col++) {
                M[i][col] = M[i][col].divide(pivot);
            }
            for (int r = i + 1; r < n; r++) {
                BigInteger factor = M[r][i];
                for (int col = i; col <= n; col++) {
                    M[r][col] = M[r][col].subtract(factor.multiply(M[i][col]));
                }
            }
        }

        BigInteger[] X = new BigInteger[n];
        for (int i = n - 1; i >= 0; i--) {
            BigInteger sum = M[i][n];
            for (int j = i + 1; j < n; j++) {
                sum = sum.subtract(M[i][j].multiply(X[j]));
            }
            X[i] = sum;
        }
        return X;
    }

    static Map<String, Object> parseJSON(String json) {
        Map<String, Object> data = new HashMap<>();
        json = json.replaceAll("[\\n\\r\\s]", "");

        String keysStr = extractBlock(json, "\"keys\":\\{", "\\}");
        int n = Integer.parseInt(extractValue(keysStr, "\"n\":", ","));
        int k = Integer.parseInt(extractValue(keysStr, "\"k\":", "}"));
        data.put("n", n);
        data.put("k", k);

        Map<Integer, Map<String, String>> roots = new HashMap<>();
        int pos = json.indexOf("\"keys\":");
        pos = json.indexOf("}", pos) + 1;

        while (pos < json.length()) {
            int keyStart = json.indexOf("\"", pos);
            if (keyStart == -1) break;
            int keyEnd = json.indexOf("\"", keyStart + 1);
            String key = json.substring(keyStart + 1, keyEnd);

            if (key.equals("keys")) {
                pos = keyEnd + 1;
                continue;
            }

            int objStart = json.indexOf("{", keyEnd);
            int objEnd = findMatchingBrace(json, objStart);
            String objStr = json.substring(objStart + 1, objEnd);

            String base = extractValue(objStr, "\"base\":\"", "\"");
            String value = extractValue(objStr, "\"value\":\"", "\"");

            roots.put(Integer.parseInt(key), Map.of("base", base, "value", value));
            pos = objEnd + 1;
        }
        data.put("roots", roots);
        return data;
    }

    static String extractBlock(String str, String startRegex, String endDelim) {
        int start = str.indexOf(startRegex.replace("\\{", "{").replace("\\}", "}"));
        if (start == -1) return "";
        start += startRegex.length() - 2;
        int end = findMatchingBrace(str, start);
        return str.substring(start + 1, end);
    }

    static int findMatchingBrace(String str, int pos) {
        int count = 0;
        for (int i = pos; i < str.length(); i++) {
            if (str.charAt(i) == '{') count++;
            else if (str.charAt(i) == '}') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    static String extractValue(String str, String key, String delim) {
        int start = str.indexOf(key);
        if (start == -1) return "";
        start += key.length();
        int end = str.indexOf(delim, start);
        if (end == -1) end = str.length();
        return str.substring(start, end);
    }

    static BigInteger solveSecretFromJson(String json) {
        Map<String, Object> parsed = parseJSON(json);
        int n = (int) parsed.get("n");
        int k = (int) parsed.get("k");
        int m = k - 1;
        Map<Integer, Map<String, String>> roots = (Map<Integer, Map<String, String>>) parsed.get("roots");

        List<Point> points = new ArrayList<>();

        List<Integer> keysSorted = new ArrayList<>(roots.keySet());
        Collections.sort(keysSorted);

        for (int i = 0; i < k; i++) {
            int x = keysSorted.get(i);
            Map<String, String> val = roots.get(x);
            int base = Integer.parseInt(val.get("base"));
            String value = val.get("value");

            BigInteger y = decodeValue(value, base);

            // Validation: re-encode and compare ignoring case for hex and base > 10
            String encoded = encodeValue(y, base);
            // Normalize both for fair comparison (lowercase)
            String valueNorm = value.toLowerCase();
            String encodedNorm = encoded.toLowerCase();

            if (valueNorm.equals(encodedNorm)) {
                System.out.println("Key " + x + " is correct");
            } else {
                System.out.println("Key " + x + " is WRONG");
                System.out.println("  Original value: " + value);
                System.out.println("  Decoded and re-encoded: " + encoded);
            }

            points.add(new Point(x, y));
        }

        BigInteger[] coeffs = solvePolynomialCoefficients(m, k, points);
        return coeffs[k - 1];
    }

    public static void main(String[] args) {
        String json1 = "{\n" +
                "    \"keys\": {\n" +
                "        \"n\": 4,\n" +
                "        \"k\": 3\n" +
                "    },\n" +
                "    \"1\": {\n" +
                "        \"base\": \"10\",\n" +
                "        \"value\": \"4\"\n" +
                "    },\n" +
                "    \"2\": {\n" +
                "        \"base\": \"2\",\n" +
                "        \"value\": \"111\"\n" +
                "    },\n" +
                "    \"3\": {\n" +
                "        \"base\": \"10\",\n" +
                "        \"value\": \"12\"\n" +
                "    },\n" +
                "    \"6\": {\n" +
                "        \"base\": \"4\",\n" +
                "        \"value\": \"213\"\n" +
                "    }\n" +
                "}";

        String json2 = "{\n" +
                "  \"keys\": {\n" +
                "      \"n\": 10,\n" +
                "      \"k\": 7\n" +
                "  },\n" +
                "  \"1\": {\n" +
                "      \"base\": \"6\",\n" +
                "      \"value\": \"13444211440455345511\"\n" +
                "  },\n" +
                "  \"2\": {\n" +
                "      \"base\": \"15\",\n" +
                "      \"value\": \"aed7015a346d63\"\n" +
                "  },\n" +
                "  \"3\": {\n" +
                "      \"base\": \"15\",\n" +
                "      \"value\": \"6aeeb69631c227c\"\n" +
                "  },\n" +
                "  \"4\": {\n" +
                "      \"base\": \"16\",\n" +
                "      \"value\": \"e1b5e05623d881f\"\n" +
                "  },\n" +
                "  \"5\": {\n" +
                "      \"base\": \"8\",\n" +
                "      \"value\": \"316034514573652620673\"\n" +
                "  },\n" +
                "  \"6\": {\n" +
                "      \"base\": \"3\",\n" +
                "      \"value\": \"2122212201122002221120200210011020220200\"\n" +
                "  },\n" +
                "  \"7\": {\n" +
                "      \"base\": \"3\",\n" +
                "      \"value\": \"20120221122211000100210021102001201112121\"\n" +
                "  },\n" +
                "  \"8\": {\n" +
                "      \"base\": \"6\",\n" +
                "      \"value\": \"20220554335330240002224253\"\n" +
                "  },\n" +
                "  \"9\": {\n" +
                "      \"base\": \"12\",\n" +
                "      \"value\": \"45153788322a1255483\"\n" +
                "  },\n" +
                "  \"10\": {\n" +
                "      \"base\": \"7\",\n" +
                "      \"value\": \"1101613130313526312514143\"\n" +
                "  }\n" +
                "}";

        System.out.println("Testcase 1:");
        BigInteger secret1 = solveSecretFromJson(json1);
        System.out.println("Secret (c): " + secret1);

        System.out.println("\nTestcase 2:");
        BigInteger secret2 = solveSecretFromJson(json2);
        System.out.println("Secret (c): " + secret2);
    }
}

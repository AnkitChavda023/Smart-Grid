package com.smartgrid.contractagent.service;

import org.springframework.stereotype.Component;

/** Word-level longest common subsequence between current and proposed clause text — O(m*n) DP, standard textbook LCS. */
@Component
public class ClauseDiffAnalyzer {

    public double similarity(String currentText, String proposedText) {
        String[] current = tokenize(currentText);
        String[] proposed = tokenize(proposedText);
        if (current.length == 0 || proposed.length == 0) {
            return 0.0;
        }

        int m = current.length;
        int n = proposed.length;
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (current[i - 1].equals(proposed[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        int lcsLength = dp[m][n];
        return (2.0 * lcsLength) / (m + n);
    }

    private String[] tokenize(String text) {
        return text == null ? new String[0] : text.trim().toLowerCase().split("\\s+");
    }
}

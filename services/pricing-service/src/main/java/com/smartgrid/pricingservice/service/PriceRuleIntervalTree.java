package com.smartgrid.pricingservice.service;

import com.smartgrid.pricingservice.domain.PriceRule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PriceRuleIntervalTree {

    private static final class Node {
        final PriceRule rule;
        final long low;
        final long high;
        long maxEnd;
        Node left;
        Node right;

        Node(PriceRule rule) {
            this.rule = rule;
            this.low = rule.getValidFrom().toEpochDay();
            this.high = rule.getValidTo().toEpochDay();
            this.maxEnd = high;
        }
    }

    private Node root;

    public void insert(PriceRule rule) {
        root = insert(root, rule);
    }

    private Node insert(Node node, PriceRule rule) {
        if (node == null) {
            return new Node(rule);
        }
        long low = rule.getValidFrom().toEpochDay();
        if (low < node.low) {
            node.left = insert(node.left, rule);
        } else {
            node.right = insert(node.right, rule);
        }
        node.maxEnd = Math.max(node.maxEnd, rule.getValidTo().toEpochDay());
        return node;
    }

    // O(h + k): h = tree height, k = number of matching intervals. Prunes the left subtree whenever its
    // highest end date can't reach the query date, and the right subtree using the BST ordering on "low".
    public List<PriceRule> findOverlapping(LocalDate date) {
        List<PriceRule> results = new ArrayList<>();
        search(root, date.toEpochDay(), results);
        return results;
    }

    private void search(Node node, long point, List<PriceRule> results) {
        if (node == null) {
            return;
        }
        if (node.left != null && node.left.maxEnd >= point) {
            search(node.left, point, results);
        }
        if (node.low <= point && point <= node.high) {
            results.add(node.rule);
        }
        if (point >= node.low) {
            search(node.right, point, results);
        }
    }
}

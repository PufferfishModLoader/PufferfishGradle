package dev.cbyrne.pufferfishmodloader.gradle.utils.versions.json;

import com.google.common.collect.Maps;
import dev.cbyrne.pufferfishmodloader.gradle.PufferfishGradle;

import java.util.Map;

public class Rule {
    private final RuleAction action;
    private final OsRulePart os;
    private final Map<String, Boolean> features;

    public Rule(RuleAction action, OsRulePart os, Map<String, Boolean> features) {
        this.action = action;
        this.os = os;
        this.features = features;
    }

    // returns null if doesn't apply, else returns the action
    public RuleAction getEffectiveAction(Map<String, Boolean> currentFeatures) {
        if (os != null && !os.appliesToCurrent()) return null;
        if (features != null) for (Map.Entry<String, Boolean> feature : features.entrySet()) {
            if (currentFeatures.getOrDefault(feature.getKey(), false) != feature.getValue()) {
                return null;
            }
        }
        return getAction();
    }

    public RuleAction getAction() {
        return action;
    }

    public OsRulePart getOs() {
        return os;
    }

    @Override
    public String toString() {
        return "Rule{" +
                "action=" + action +
                ", os=" + os +
                ", features=" + features +
                '}';
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public static boolean allow(Rule[] rules, Map<String, Boolean> currentFeatures) {
        if (rules == null) return true;
        boolean allow = rules.length == 0;
        for (Rule rule : rules) {
            RuleAction action = rule.getEffectiveAction(currentFeatures);
            if (action != null) {
                allow = action == RuleAction.ALLOW;
            }
        }
        return allow;
    }
}

package com.sentinel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;

/** Generates formatted plain text and HTML Incident Reports. */
public final class IncidentReport {
    private IncidentReport() {}

    public static String generateText(Scorecard scorecard, DefenseMode defense,
                                      List<Alert> alerts, List<TimelineEntry> timeline) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            pw.println("================================================================================");
            pw.println("                   SENTINEL INCIDENT INVESTIGATION REPORT                       ");
            pw.println("================================================================================");
            pw.println("Generated At: " + Instant.now());
            pw.println();
            pw.println("--- 1. EXECUTIVE SUMMARY & SCORECARD ---");
            pw.println("Evaluation Grade : " + scorecard.grade());
            pw.println("Attacks Launched : " + scorecard.getAttacksLaunched());
            pw.println("Attacks Detected : " + scorecard.getAttacksDetected());
            pw.println("Time to Detect   : " + scorecard.getTimeToDetectMs() + " ms");
            pw.println("False Positives  : " + scorecard.getFalsePositives());
            pw.println("Compromises      : " + scorecard.getCompromises());
            pw.println();
            pw.println("--- 2. ACTIVE DEFENSE METRICS ---");
            pw.println("Defense Mode Enabled: " + defense.isEnabled());
            pw.println("Auto-blocks Issued  : " + defense.getBlocksIssued());
            pw.println("Blocked IPs         : " + String.join(", ", defense.getBlockedIps()));
            pw.println();
            pw.println("--- 3. DETECTED ALERTS & MITRE ATT&CK MAPPINGS ---");
            for (Alert a : alerts) {
                pw.printf("[%s] %s | Severity: %s | MITRE: %s (%s) | IP: %s | User: %s%n",
                    a.timestamp(), a.ruleName(), a.severity(), a.mitreTag(), a.mitreLabel(), a.ip(), a.username());
                pw.printf("   Why    : %s%n", a.why());
                pw.printf("   Defend : %s%n", a.defend());
            }
            pw.println();
            pw.println("--- 4. RECOMMENDED REMEDIATION ---");
            pw.println("1. Enforce multi-factor authentication (MFA) across all user accounts.");
            pw.println("2. Implement rate limiting and automatic IP throttling at the API gateway.");
            pw.println("3. Ensure blocked IP lists are synchronized with border firewalls.");
            pw.println("================================================================================");
        }
        return sw.toString();
    }

    public static String generateHtml(Scorecard scorecard, DefenseMode defense,
                                      List<Alert> alerts, List<TimelineEntry> timeline) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Sentinel Incident Report</title>")
          .append("<style>body{font-family:sans-serif;background:#0f172a;color:#f8fafc;padding:30px;line-height:1.5;}")
          .append("h1,h2{color:#38bdf8;}table{width:100%;border-collapse:collapse;margin:15px 0;}")
          .append("th,td{border:1px solid #334155;padding:8px 12px;text-align:left;}th{background:#1e293b;}")
          .append(".badge{padding:3px 8px;border-radius:4px;font-size:12px;font-weight:bold;}")
          .append(".CRITICAL{background:#ef4444;color:white;}.HIGH{background:#f97316;color:white;}")
          .append(".MEDIUM{background:#eab308;color:black;}.LOW{background:#3b82f6;color:white;}")
          .append("</style></head><body>")
          .append("<h1>Sentinel Incident Report</h1>")
          .append("<p>Report Generated: ").append(Instant.now()).append("</p>")
          .append("<h2>Detection Scorecard</h2>")
          .append("<p>Grade: <strong>").append(scorecard.grade()).append("</strong> | ")
          .append("Attacks Launched: ").append(scorecard.getAttacksLaunched()).append(" | ")
          .append("Detected: ").append(scorecard.getAttacksDetected()).append(" | ")
          .append("Time to Detect: ").append(scorecard.getTimeToDetectMs()).append("ms</p>")
          .append("<h2>Active Defense</h2>")
          .append("<p>Defense Mode: <strong>").append(defense.isEnabled() ? "ON" : "OFF").append("</strong> | ")
          .append("Auto-blocks: ").append(defense.getBlocksIssued()).append("</p>")
          .append("<h2>Alerts & MITRE ATT&CK</h2><table>")
          .append("<tr><th>Timestamp</th><th>Rule</th><th>Severity</th><th>MITRE</th><th>IP</th><th>Target</th><th>Remediation</th></tr>");

        for (Alert a : alerts) {
            sb.append("<tr>")
              .append("<td>").append(a.timestamp()).append("</td>")
              .append("<td>").append(a.ruleName()).append("</td>")
              .append("<td><span class='badge ").append(a.severity()).append("'>").append(a.severity()).append("</span></td>")
              .append("<td>").append(a.mitreTag()).append(" ").append(a.mitreLabel()).append("</td>")
              .append("<td>").append(a.ip()).append("</td>")
              .append("<td>").append(a.username()).append("</td>")
              .append("<td>").append(a.defend()).append("</td>")
              .append("</tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }
}

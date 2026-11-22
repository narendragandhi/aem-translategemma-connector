(function ($, document) {
    "use strict";

    var AUDIT_API = "/bin/translation-gemma/audit";
    var OVERLAY_SELECTOR = ".foundation-collection-item";

    function applyGemmaInsights() {
        $.get(AUDIT_API, { limit: 100 }, function (data) {
            if (!data || !data.length) return;

            $(OVERLAY_SELECTOR).each(function () {
                var $item = $(this);
                var path = $item.data("foundation-collection-item-id");
                
                // Find matching audit entry for this path
                var entry = data.find(function (e) { return e.path === path; });
                
                if (entry && !$item.find(".gemma-insight-badge").length) {
                    var sentiment = entry.sentiment ? entry.sentiment.sentiment : "NEUTRAL";
                    var confidence = entry.sentiment ? Math.round(entry.sentiment.confidence * 100) : 0;
                    var reasoning = entry.sentiment ? entry.sentiment.reasoning : "No reasoning provided.";
                    
                    var color = confidence > 85 ? "#28a745" : (confidence > 60 ? "#ffc107" : "#dc3545");

                    // Build with text()/attr() so model-provided strings can never inject markup
                    var $badge = $("<div class=\"gemma-insight-badge\">")
                        .attr("title", reasoning)
                        .text("Gemma: " + sentiment + " (" + confidence + "%)")
                        .css({
                            display: "inline-block",
                            padding: "2px 6px",
                            borderRadius: "4px",
                            color: "#fff",
                            fontSize: "10px",
                            marginLeft: "10px",
                            backgroundColor: color
                        });

                    // Inject badge into the title or status area of the list item
                    $item.find(".foundation-collection-item-title").append($badge);
                }
            });
        });
    }

    var pollStarted = false;

    $(document).on("foundation-contentloaded", function () {
        applyGemmaInsights();
        if (!pollStarted) {
            pollStarted = true;
            setInterval(applyGemmaInsights, 30000);
        }
    });

})(Granite.$, document);

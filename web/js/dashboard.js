/* ============================================================
   dashboard.js — D3 v7 force-directed visualization of the
   friend-suggestion algorithm.

   Reads the global GRAPH object injected inline by dashboard.jsp
   (the controller's request attributes). No Fetch, no API.
   ============================================================ */
(function () {
    "use strict";

    const COLORS = {
        selected:   "#1877f2",
        friend:     "#42b72a",
        suggestion: "#f7b928",
        bridge:     "#8a3ffc",
        other:      "#bcc0c4"
    };

    // ---- lookups -------------------------------------------------
    const idToName = new Map(GRAPH.nodes.map(n => [n.id, n.name]));
    const directSet = new Set(GRAPH.directFriends || []);
    const suggestionMap = new Map((GRAPH.suggestions || []).map(s => [s.id, s.mutual]));
    const mutuals = GRAPH.mutualsBySuggested || {};
    const selectedId = GRAPH.selectedId; // number or null

    function roleOf(id) {
        if (id === selectedId) return "selected";
        if (suggestionMap.has(id)) return "suggestion";
        if (directSet.has(id)) return "friend";
        return "other";
    }

    function radiusOf(id) {
        if (id === selectedId) return 16;
        if (suggestionMap.has(id)) return 9 + Math.min(suggestionMap.get(id), 6) * 2.5; // size by mutuals
        if (directSet.has(id)) return 9;
        return 6.5;
    }

    // ---- graph metrics ------------------------------------------
    (function metrics() {
        const V = GRAPH.nodes.length;
        const E = GRAPH.links.length;
        const avgDeg = V ? (2 * E / V) : 0;
        const density = V > 1 ? (2 * E) / (V * (V - 1)) : 0;
        document.getElementById("m-nodes").textContent = V;
        document.getElementById("m-edges").textContent = E;
        document.getElementById("m-deg").textContent = avgDeg.toFixed(2);
        document.getElementById("m-density").textContent = density.toFixed(3);
    })();

    // ---- highlight bookkeeping ----------------------------------
    // d3-force rewrites link.source/target from ids to node objects; nid() reads
    // the id either way (used before/after the simulation resolves the links).
    const nid = (e) => (e && typeof e === "object") ? e.id : e;
    const edgeKey = (a, b) => (a < b ? a + "-" + b : b + "-" + a);

    // For each suggestion, the set of edges along selected → mutual → suggested.
    const highlightEdges = {};   // sid -> Set(edgeKey)
    const highlightNodes = {};   // sid -> Set(id)
    Object.keys(mutuals).forEach(k => {
        const sid = Number(k);
        const ms = mutuals[k] || [];
        const eset = new Set();
        const nset = new Set([selectedId, sid]);
        ms.forEach(m => {
            nset.add(m);
            eset.add(edgeKey(selectedId, m));
            eset.add(edgeKey(m, sid));
        });
        highlightEdges[sid] = eset;
        highlightNodes[sid] = nset;
    });

    // ============================================================
    //  D3 force simulation
    // ============================================================
    const container = document.getElementById("viz");
    const width = container.clientWidth;
    const height = container.clientHeight;

    const svg = d3.select("#viz").append("svg")
        .attr("viewBox", [0, 0, width, height]);

    const root = svg.append("g");

    svg.call(d3.zoom()
        .scaleExtent([0.3, 4])
        .on("zoom", (event) => root.attr("transform", event.transform)));

    // Deep-copy links so the simulation can replace ids with node refs.
    const links = GRAPH.links.map(l => Object.assign({}, l));
    const nodes = GRAPH.nodes.map(n => Object.assign({}, n));

    const simulation = d3.forceSimulation(nodes)
        .force("link", d3.forceLink(links).id(d => d.id).distance(70).strength(0.25))
        .force("charge", d3.forceManyBody().strength(-260))
        .force("center", d3.forceCenter(width / 2, height / 2))
        .force("collide", d3.forceCollide().radius(d => radiusOf(d.id) + 6));

    const link = root.append("g")
        .selectAll("line")
        .data(links)
        .join("line")
        .attr("class", "link")
        .attr("data-key", d => edgeKey(nid(d.source), nid(d.target)));

    const node = root.append("g")
        .selectAll("g")
        .data(nodes)
        .join("g")
        .attr("class", "node")
        .call(drag(simulation));

    node.append("circle")
        .attr("r", d => radiusOf(d.id))
        .attr("fill", d => COLORS[roleOf(d.id)]);

    node.append("text")
        .attr("x", d => radiusOf(d.id) + 4)
        .attr("y", 4)
        .text(d => d.name);

    // Hovering a suggestion node explains it.
    node.on("mouseenter", (event, d) => {
        if (suggestionMap.has(d.id)) highlight(d.id);
    }).on("mouseleave", () => { if (!pinned) clearHighlight(); });

    node.on("click", (event, d) => {
        if (!suggestionMap.has(d.id)) return;
        if (pinned === d.id) { pinned = null; clearHighlight(); }
        else { pinned = d.id; highlight(d.id); }
    });

    simulation.on("tick", () => {
        link.attr("x1", d => d.source.x).attr("y1", d => d.source.y)
            .attr("x2", d => d.target.x).attr("y2", d => d.target.y);
        node.attr("transform", d => `translate(${d.x},${d.y})`);
    });

    function drag(sim) {
        return d3.drag()
            .on("start", (event, d) => {
                if (!event.active) sim.alphaTarget(0.3).restart();
                d.fx = d.x; d.fy = d.y;
            })
            .on("drag", (event, d) => { d.fx = event.x; d.fy = event.y; })
            .on("end", (event, d) => {
                if (!event.active) sim.alphaTarget(0);
                d.fx = null; d.fy = null;
            });
    }

    // ============================================================
    //  Highlighting the "why" — selected → mutual → suggested
    // ============================================================
    let pinned = null;

    function highlight(sid) {
        const keepNodes = highlightNodes[sid] || new Set([selectedId, sid]);
        const keepEdges = highlightEdges[sid] || new Set();

        node.classed("dim", d => !keepNodes.has(d.id));
        node.select("circle").attr("fill", d =>
            (d.id !== selectedId && d.id !== sid && keepNodes.has(d.id))
                ? COLORS.bridge                 // recolor mutual friends as bridges
                : COLORS[roleOf(d.id)]);
        link.classed("hl", function () {
            return keepEdges.has(this.getAttribute("data-key"));
        });

        setActiveRank(sid);
        const name = idToName.get(sid);
        const cnt = suggestionMap.get(sid);
        document.getElementById("viz-caption").innerHTML =
            "Showing <b style='color:#b8860b'>" + escapeHtml(name) + "</b> — reachable through " +
            cnt + " mutual friend" + (cnt === 1 ? "" : "s") + " (purple bridges).";
    }

    function clearHighlight() {
        node.classed("dim", false);
        node.select("circle").attr("fill", d => COLORS[roleOf(d.id)]);
        link.classed("hl", false);
        setActiveRank(null);
        document.getElementById("viz-caption").innerHTML =
            "Hover a suggested (orange) node to light up the mutual-friend paths that justify it.";
    }

    // ============================================================
    //  Side-panel ranked list
    // ============================================================
    (function buildRankList() {
        const ol = document.getElementById("ranklist");
        const hint = document.getElementById("rank-hint");
        const suggestions = GRAPH.suggestions || [];

        if (selectedId === null) {
            hint.textContent = "Select a focus user to compute suggestions.";
            ol.innerHTML = '<li class="empty">No user selected.</li>';
            return;
        }
        if (suggestions.length === 0) {
            hint.textContent = "For " + escapeHtml(idToName.get(selectedId)) + ".";
            ol.innerHTML = '<li class="empty">No suggestions — not enough mutual links yet.</li>';
            return;
        }

        hint.innerHTML = "For <b>" + escapeHtml(idToName.get(selectedId)) + "</b>, ranked by mutual count.";
        suggestions.forEach(s => {
            const sharedNames = (mutuals[s.id] || [])
                .map(m => idToName.get(m)).filter(Boolean);
            const li = document.createElement("li");
            li.dataset.sid = s.id;
            li.innerHTML =
                '<span class="who">' + escapeHtml(idToName.get(s.id)) +
                    '<small class="shared">via ' +
                    (sharedNames.length ? sharedNames.map(escapeHtml).join(", ") : "—") +
                    '</small></span>' +
                '<span class="badge">' + s.mutual + '</span>';
            li.addEventListener("mouseenter", () => { if (!pinned) highlight(s.id); });
            li.addEventListener("mouseleave", () => { if (!pinned) clearHighlight(); });
            li.addEventListener("click", () => {
                if (pinned === s.id) { pinned = null; clearHighlight(); }
                else { pinned = s.id; highlight(s.id); }
            });
            ol.appendChild(li);
        });
    })();

    function setActiveRank(sid) {
        document.querySelectorAll("#ranklist li").forEach(li => {
            li.classList.toggle("active", sid !== null && Number(li.dataset.sid) === sid);
        });
    }

    function escapeHtml(s) {
        return String(s == null ? "" : s)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;")
            .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }
})();

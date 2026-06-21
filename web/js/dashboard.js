/* ============================================================
   dashboard.js — D3 v7 force-directed visualization of the
   friend-suggestion algorithm.

   Reads the global GRAPH object injected inline by dashboard.jsp
   (the controller's request attributes). No Fetch, no API.
   ============================================================ */
(function () {
    "use strict";

    // GRAPH is injected inline by dashboard.jsp (window.GRAPH). Declare it
    // explicitly here so the dependency is visible and a missing/empty inline
    // block degrades gracefully instead of throwing "GRAPH is not defined".
    const GRAPH = window.GRAPH || {
        nodes: [], links: [], selectedId: null,
        directFriends: [], suggestions: [], mutualsBySuggested: {}
    };

    // ============================================================
    //  Focus user search
    // ============================================================
    const searchInput = document.getElementById("focusUserSearch");
    const dropdown = document.getElementById("focusUserDropdown");
    const hiddenUserId = document.getElementById("selectedUserId");
    const focusUserForm = document.getElementById("focusUserForm");

    // Populate search dropdown with user names
    function updateSearchDropdown(query) {
        const q = query.trim().toLowerCase();
        const userList = document.querySelectorAll("#focusUserDropdown li");
        let visibleCount = 0;

        userList.forEach(li => {
            const searchText = li.getAttribute("data-search");
            const match = searchText.indexOf(q) !== -1;
            li.style.display = match ? "" : "none";
            if (match) visibleCount++;
        });

        dropdown.style.display = q.length > 0 && visibleCount > 0 ? "" : "none";
    }

    if (searchInput) {
        // Build initial dropdown list
        GRAPH.nodes.forEach(u => {
            const li = document.createElement("li");
            li.setAttribute("data-search", `${u.name.toLowerCase()} ${u.id}`);
            li.innerHTML = `<strong>[${u.id}]</strong> ${escapeHtml(u.name)}`;
            li.addEventListener("click", () => {
                hiddenUserId.value = u.id;
                searchInput.value = `[${u.id}] ${u.name}`;
                dropdown.style.display = "none";
                focusUserForm.submit();
            });
            dropdown.appendChild(li);
        });

        // Update dropdown on input
        searchInput.addEventListener("input", (e) => updateSearchDropdown(e.target.value));
        searchInput.addEventListener("focus", (e) => {
            if (e.target.value.length > 0) {
                updateSearchDropdown(e.target.value);
            }
        });

        // Set initial search value if user is selected
        if (GRAPH.selectedId !== null) {
            const selectedNode = GRAPH.nodes.find(n => n.id === GRAPH.selectedId);
            const selectedName = selectedNode ? selectedNode.name : "";
            searchInput.value = `[${GRAPH.selectedId}] ${selectedName}`;
        }

        // Close dropdown when clicking outside
        document.addEventListener("click", (e) => {
            if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
                dropdown.style.display = "none";
            }
        });
    }

    function escapeHtml(s) {
        return String(s == null ? "" : s)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;")
            .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }

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

    // highlightEdges/highlightNodes are populated AFTER the rendered graph is
    // known (see buildHighlightSets below), so the glow can only ever light up
    // bridges/edges that are actually drawn and linked back to the focus user.
    const highlightEdges = {};   // sid -> Set(edgeKey)
    const highlightNodes = {};   // sid -> Set(id)

    // ============================================================
    //  D3 force simulation
    // ============================================================
    const container = document.getElementById("viz");
    const width = container.clientWidth;
    const height = container.clientHeight;

    const svg = d3.select("#viz").append("svg")
        .attr("viewBox", [0, 0, width, height]);

    const root = svg.append("g");

    const zoomBehavior = d3.zoom()
        .scaleExtent([0.3, 4])
        .on("zoom", (event) => root.attr("transform", event.transform));
    svg.call(zoomBehavior);

    // ============================================================
    //  Focused view = clean depth-2 "why" graph (not the whole graph)
    // ============================================================
    // When a user is focused we render ONLY a tidy two-level structure:
    //   focus user (centre)
    //     → mutual-friend "bridges"  (spokes — every shown friend links to focus)
    //         → suggestions          (outer ring, justified by their bridges)
    // ONLY two kinds of edges are drawn: focus→bridge and bridge→suggestion.
    // Friend↔friend and suggestion↔suggestion edges are deliberately omitted so
    // nothing trails off into chains that aren't anchored to the focus user.
    // This guarantees every rendered node connects back to the focus user.
    const MAX_BRIDGES = 5; // max bridges drawn per suggestion (keeps it readable)

    function buildEgoGraph() {
        const links = [];
        const usedBridges = new Set();
        const shownSuggestions = new Set();

        suggestionMap.forEach((_, sid) => {
            // Valid bridges = mutual friends that really are direct friends of
            // the focus user. Cap per suggestion so no suggestion fans out wide.
            const bridges = (mutuals[sid] || []).filter(m => directSet.has(m));
            bridges.slice(0, MAX_BRIDGES).forEach(m => {
                links.push({ source: m, target: sid });
                usedBridges.add(m);
                shownSuggestions.add(sid);
            });
        });

        // Spoke from the focus user to every bridge actually used — this is what
        // anchors each shown friend (and, through it, each suggestion) to focus.
        usedBridges.forEach(m => links.push({ source: selectedId, target: m }));

        const keep = new Set([selectedId]);
        usedBridges.forEach(id => keep.add(id));
        shownSuggestions.forEach(id => keep.add(id));

        return {
            nodes: GRAPH.nodes.filter(n => keep.has(n.id)).map(n => Object.assign({}, n)),
            links: links
        };
    }

    const graphData = (selectedId !== null) ? buildEgoGraph() : {
        nodes: GRAPH.nodes.map(n => Object.assign({}, n)),
        links: GRAPH.links.map(l => Object.assign({}, l))
    };

    // Deep-copied so the simulation can replace ids with node refs.
    const links = graphData.links;
    const nodes = graphData.nodes;

    // Populate the highlight sets from the edges that are ACTUALLY rendered.
    // For each suggestion, a mutual friend m only glows when BOTH the
    // focus→m and m→suggestion edges survive in the drawn graph — so the glow
    // always traces a real, connected path back to the focus user (no orphan
    // friends lighting up, no broken links).
    (function buildHighlightSets() {
        const rendered = new Set(links.map(l => edgeKey(nid(l.source), nid(l.target))));
        Object.keys(mutuals).forEach(k => {
            const sid = Number(k);
            const eset = new Set();
            const nset = new Set([selectedId, sid]);
            (mutuals[k] || []).forEach(m => {
                const keyToFocus = edgeKey(selectedId, m);
                const keyToSugg = edgeKey(m, sid);
                if (rendered.has(keyToFocus) && rendered.has(keyToSugg)) {
                    nset.add(m);
                    eset.add(keyToFocus);
                    eset.add(keyToSugg);
                }
            });
            highlightEdges[sid] = eset;
            highlightNodes[sid] = nset;
        });
    })();

    // Seed positions across a filled DISC (radius = sqrt(rand) for uniform area)
    // rather than a thin ring — a ring is a stable equilibrium that leaves the
    // centre permanently empty.
    const seedR = Math.min(width, height) * 0.4;
    nodes.forEach(n => {
        const angle = Math.random() * 2 * Math.PI;
        const radius = Math.sqrt(Math.random()) * seedR;
        n.x = width / 2 + radius * Math.cos(angle);
        n.y = height / 2 + radius * Math.sin(angle);
    });

    const simulation = d3.forceSimulation(nodes)
        .alpha(0.5)
        .alphaDecay(0.0228)
        .force("link", d3.forceLink(links).id(d => d.id).distance(70).strength(0.25))
        .force("charge", d3.forceManyBody().strength(-260))
        .force("center", d3.forceCenter(width / 2, height / 2))
        // Mild per-node pull toward centre breaks the empty-ring equilibrium and
        // fills the middle (forceCenter alone only re-centres the centroid).
        .force("x", d3.forceX(width / 2).strength(0.06))
        .force("y", d3.forceY(height / 2).strength(0.06))
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

    // Pan/zoom the camera so the given node ids are centered and fully framed.
    function fitView(targetIds) {
        // Re-measure at call time; fall back to the seed dims, then a sane default,
        // so a stale/zero initial measurement can never break the fit.
        const vw = container.clientWidth || width || 600;
        const vh = container.clientHeight || height || 620;

        // Bounding box of the target nodes' settled positions.
        let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
        nodes.forEach(n => {
            if (targetIds.has(n.id)) {
                minX = Math.min(minX, n.x);
                maxX = Math.max(maxX, n.x);
                minY = Math.min(minY, n.y);
                maxY = Math.max(maxY, n.y);
            }
        });

        // Nothing to frame (empty target / empty graph) → leave the view alone.
        if (minX === Infinity) return;

        const bbox = {
            x: minX, y: minY,
            width: maxX - minX || 1,   // single point → avoid divide-by-zero
            height: maxY - minY || 1
        };

        // Fit the bbox with ~30% padding, clamped to the zoom scaleExtent.
        const padding = 0.3;
        const paddedWidth = bbox.width * (1 + padding);
        const paddedHeight = bbox.height * (1 + padding);
        const scale = Math.min(vw / paddedWidth, vh / paddedHeight);
        const clampedScale = Math.max(0.3, Math.min(scale, 4));

        // Center the bbox midpoint in the viewport.
        const centerX = bbox.x + bbox.width / 2;
        const centerY = bbox.y + bbox.height / 2;
        const tx = vw / 2 - centerX * clampedScale;
        const ty = vh / 2 - centerY * clampedScale;

        const transform = d3.zoomIdentity.translate(tx, ty).scale(clampedScale);
        svg.transition().duration(500).call(zoomBehavior.transform, transform);
    }

    // Fit the focused neighbourhood when a user is selected, otherwise the whole graph.
    function autoFit() {
        const targetIds = new Set();
        if (selectedId !== null) {
            targetIds.add(selectedId);
            directSet.forEach(id => targetIds.add(id));
            suggestionMap.forEach((_, id) => targetIds.add(id));
        } else {
            nodes.forEach(n => targetIds.add(n.id));
        }
        fitView(targetIds);
    }

    // Re-fit precisely once the layout settles, plus an early fallback so the
    // camera never sits idle on a blank frame while alpha decays.
    simulation.on("end", autoFit);
    setTimeout(autoFit, 600);

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
})();

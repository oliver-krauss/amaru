<#macro dependencies>
    <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/viz.js/2.1.2/viz.js"></script>
    <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/viz.js/2.1.2/full.render.js"></script>
</#macro>
<#macro vizscript>
    <script>
        // start listening to keypresses
        document.addEventListener("keydown", logKeyDown);
        document.addEventListener("keyup", logKeyUp);
        var pressCtrl = false;

        function logKeyDown(e) {
            if (e.keyCode === 17) {
                pressCtrl = true;
            }
        }

        function logKeyUp(e) {
            if (e.keyCode === 17) {
                pressCtrl = false;
            }
        }

        var idList = [];
        var callerList = [];

        function toggleNode(caller, toggleId) {

            if (!Array.isArray(toggleId)) {
                if (toggleId.startsWith('_')) {
                    toggleId = toggleId.substring(1).split('_');
                } else {
                    toggleId = [toggleId];
                }
            }
            // clean id list
            if (!pressCtrl) {
                idList = toggleId
                callerList = [caller];
            } else {
                Array.prototype.push.apply(idList, toggleId);
                callerList.push(caller);
            }

            // mark all nodes that are in ID list as blue
            Array.from(document.getElementsByClassName("node")).forEach(function (node) {
                let id = node.children[0].innerHTML;
                if (id.startsWith('_')) {
                    let idArray = id.substring(1).split('_');
                    if (idList.some(value => idArray.includes(value))) {
                        node.children[1].style.fill = "lightblue"
                    } else {
                        node.children[1].style.fill = "none"
                    }
                } else {
                    if (idList.includes(id)) {
                        node.children[1].style.fill = "lightblue"
                    } else {
                        node.children[1].style.fill = "none"
                    }
                }
            });

            // mark all nodes that we clicked on originally as red
            callerList.forEach(function (caller) {
                if (caller.children[1].nodeName === "ellipse") {
                    caller.children[1].style.fill = "lightsalmon"
                } else {
                    Array.from(caller.children).forEach(function (g) {
                        if (g.classList.contains("node")) {
                            g.children[1].style.fill = "lightsalmon"
                        }
                    })
                }
            });
        }

        var viz = new Viz();
        Array.from(document.getElementsByClassName("graphviz")).forEach(function (graph) {
            viz.renderSVGElement(graph.textContent)
                .then(function (element) {
                    // add onclick to nodes
                    Array.from(element.children[0].children).forEach(function (node) {
                        if (node.classList.contains("node")) {
                            let id = node.children[0].innerHTML;
                            node.onclick = function (event) {
                                event.stopPropagation();
                                toggleNode(node, id);
                            }
                        }
                    });
                    // add onclick to graph -> either as graph or as pattern
                    if (graph.parentNode.children[0].nodeName === "TABLE" && graph.parentNode.children[0].attributes.idList != null) {
                        let idList = graph.parentNode.children[0].attributes.idList.value.split(",");
                        element.children[0].onclick = function () {
                            toggleNode(element.children[0], idList);
                        };
                    } else {
                        let ids = [];
                        // collect ids
                        Array.from(element.children[0].children).forEach(function (node) {
                            if (node.classList.contains("node")) {
                                ids.push(node.children[0].innerHTML);
                            }
                        });
                        element.children[0].onclick = function () {
                            toggleNode(element.children[0], ids);
                        };
                    }
                    graph.parentNode.replaceChild(element, graph);
                });
        });
    </script>
</#macro>
<#macro tabscript>
    <script>
        function openTab(evt, tabName, level) {
            // Declare all variables
            var i, tabcontent, tablinks;

            // Get all elements with class="tabcontent" and hide them
            tabcontent = document.getElementsByClassName("tabcontent-" + level);
            for (i = 0; i < tabcontent.length; i++) {
                tabcontent[i].style.display = "none";
            }

            // Get all elements with class="tablinks" and remove the class "active"
            tablinks = document.getElementsByClassName("tablinks-" + level);
            for (i = 0; i < tablinks.length; i++) {
                tablinks[i].className = tablinks[i].className.replace(" active", "");
            }

            // Show the current tab, and add an "active" class to the button that opened the tab
            document.getElementById(tabName).style.display = "block";
            evt.currentTarget.className += " active";
        }

        // Get the element with id="defaultOpen" and click on it
        document.getElementById("defaultTab").click();
    </script>
</#macro>


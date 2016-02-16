     maxPart = 0;

     function addBox(){

        for(i = 0; i <= maxPart; i++){

            var aktTable = document.getElementById("questions_part" + i);
            var tc = aktTable.rows.length - 2;
            var table_counter = tc + 1;


            var actBox = document.getElementById("question" + i + "_" + tc + "_2");

            if( !(actBox.value.trim() == "") ){

                var newBox1 = document.createElement("input");
                with(newBox1){
                setAttribute("type","text");
                setAttribute("class","form-control");
                setAttribute("placeholder","Sentence 1");
                setAttribute("id","question" + i + "_" + table_counter + "_1");
                setAttribute("name", "question" + i + "_" + table_counter + "_1");
                }

                var newBox2 = document.createElement("input");
                with(newBox2){
                  setAttribute("type","text");
                  setAttribute("class","form-control");
                  setAttribute("placeholder","Sentence 2");
                  setAttribute("id","question" + i + "_" + table_counter + "_2");
                  setAttribute("name","question" + i + "_" + table_counter + "_2");
                  setAttribute("onkeydown","addBox()");
                }

                //Tabelle

                var table = document.getElementById("questions_part" + i);
                var rowCount = table.rows.length;
                var row = table.insertRow(rowCount);

                var zelle1 = row.insertCell(0);
                zelle1.appendChild(newBox1);

                var zelle2 = row.insertCell(1);
                zelle2.appendChild(newBox2);
                }                                   /* If - End */
        }                                           /* For-Loop End */
     }                                              /* Funcion End */

     function addPart(){
        var part = document.getElementById("allquestions");
        maxPart++;

        part.appendChild(document.createElement("br"));

        var newLabel = document.createElement("label");
        with(newLabel){
          setAttribute("for", "questions_part" + maxPart);
          innerHTML = "Part " + maxPart;
        }

        part.appendChild(newLabel);


        var newTable = document.createElement("table");
        newTable.setAttribute("border", "2");
        newTable.setAttribute("id", "questions_part" + maxPart);
        newTable.setAttribute("width","100%");


        tr = newTable.insertRow();

        var newBox1 = document.createElement("input");
          with(newBox1){
            setAttribute("type","text");
            setAttribute("class","form-control");
            setAttribute("placeholder","Sentence 1");
            setAttribute("id","question" + maxPart + "_" + 0 + "_1");
            setAttribute("name", "question" + maxPart + "_" + 0 + "_1");
          }

        var newBox2 = document.createElement("input");
          with(newBox2){
            setAttribute("type","text");
            setAttribute("class","form-control");
            setAttribute("placeholder","Sentence 2");
            setAttribute("id","question" + maxPart + "_" + 0 + "_2");
            setAttribute("name","question" + maxPart + "_" + 0 + "_2");
            setAttribute("onkeydown","addBox()");
          }


        tr.insertCell().appendChild(newBox2);
        tr.insertCell().appendChild(newBox1);

        var tr = newTable.insertRow();

        var th = document.createElement("th");
        th.appendChild(document.createTextNode("Satz 1"));
        tr.appendChild(th);

        th = document.createElement("th");
        th.appendChild(document.createTextNode("Satz 2"));
        tr.appendChild(th);


        part.appendChild(newTable);

     }
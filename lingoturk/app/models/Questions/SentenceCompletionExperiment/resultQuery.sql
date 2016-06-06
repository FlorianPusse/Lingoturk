SELECT *
FROM   (SELECT Cast(item_nr AS INT),
               Cast(list AS INT),
               item_length,
               item_type,
               workerid,
               SentenceCompletionResults.answer
        FROM   SentenceCompletionResults
               JOIN questions using (questionid)
        ORDER  BY list,
                  item_nr,
                  workerid) AS tmp
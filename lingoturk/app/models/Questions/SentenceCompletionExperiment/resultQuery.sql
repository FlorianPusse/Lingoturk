SELECT *
FROM   (SELECT Cast(SentenceCompletion_itemNr AS INT),
               Cast(SentenceCompletion_list AS INT),
               SentenceCompletion_itemLength,
               SentenceCompletion_itemType,
               workerid,
               SentenceCompletionResults.answer
        FROM   SentenceCompletionResults
               JOIN questions using (questionid)
        ORDER  BY SentenceCompletion_list,
                  SentenceCompletion_itemNr,
                  workerid) AS tmp
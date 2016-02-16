SELECT *
FROM   (SELECT Cast(item_nr AS INT),
               Cast(list AS INT),
               item_length,
               item_type,
               workerid,
               storycompletionresultv2.answer
        FROM   storycompletionresultv2
               JOIN questions using (questionid)
        ORDER  BY list,
                  item_nr,
                  workerid) AS tmp
SELECT
   scenarioName,
   workerId,
   acceptTime,
   submissionTime,
   workingTimes,
   lhs_script_id,
   rhs_script_id,
   lhs_item_slot,
   CASE
      WHEN noLinkingPossible THEN '-1'
      WHEN rhs_item_slot IS NOT NULL THEN rhs_item_slot || ''
      WHEN before_slot IS NULL THEN 'after_' || after_slot
      WHEN after_slot IS NULL THEN 'before_' || before_slot
      ELSE 'between_' || after_slot || '_' || before_slot
   END AS result,
   goldenStandart || '' AS goldenStandart
FROM
   (SELECT
      DISTINCT *
   FROM (SELECT
         LinkingResult.workerId,
         workingTimes,
         getScenarioName(lhs_script) AS scenarioName,
         getScriptId(lhs_script) AS lhs_script_id,
         getScriptId(rhs_script) AS rhs_script_id,
         getItemSlot(lhs_item) AS lhs_item_slot,
         getItemSlot(rhs_item) AS rhs_item_slot,
         getItemSlot(before) AS before_slot,
         getItemSlot(after) AS after_slot,
         noLinkingPossible,
         getStandart(lhs_item,getScriptId(rhs_script)) AS goldenStandart,
         LinkingResult.timestamp AS submissionTime,
         workers_participateIn_parts.timestamp AS acceptTime
      FROM
         LinkingV1Results
      JOIN
         workers_participateIn_parts
      ON LinkingResult.workerId = workers_participateIn_parts.workerId AND lhs_script = questionId AND rhs_script = questionId2) AS tmp) as tmp2
ORDER BY
      scenarioName,
      workerId,
      lhs_script_id,
      rhs_script_id,
      lhs_item_slot;
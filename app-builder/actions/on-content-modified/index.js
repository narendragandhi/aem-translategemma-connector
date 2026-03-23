const { Core } = require('@adobe/aio-sdk');
const fetch = require('node-fetch');

/**
 * App Builder action to handle AEM content modification events.
 */
async function main(params) {
  const logger = Core.Logger('on-content-modified', { level: params.LOG_LEVEL || 'info' });
  
  try {
    const { event, aem_host, aem_token } = params;
    
    if (!event || !event.data || !event.data.payload) {
      return { statusCode: 400, body: { error: 'Invalid event payload' } };
    }

    const payloadPath = event.data.payload.path;
    logger.info(`Received content modification event for: ${payloadPath}`);

    // 1. Authenticate and call AEM Intelligence API
    // (Note: Endpoint to be implemented in AEM project)
    const apiUrl = `${aem_host}/bin/translategemma/analyze`;
    
    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${aem_token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        path: payloadPath,
        types: ['sentiment', 'compliance']
      })
    });

    if (!response.ok) {
      throw new Error(`AEM API failed with status ${response.status}`);
    }

    const result = await response.json();
    logger.info(`Successfully analyzed content: ${JSON.stringify(result)}`);

    return { 
      statusCode: 200, 
      body: { 
        message: 'Content analysis orchestrated successfully',
        path: payloadPath,
        result: result 
      } 
    };

  } catch (error) {
    logger.error(error);
    return Core.Response.errorResponse(500, error.message, logger);
  }
}

exports.main = main;

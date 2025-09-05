import BaseElement from './api/BaseElement.js';
import CoordinatorApp from './CoordinatorApp.js';

export class RegistryTableElement extends BaseElement 
{
	refresher = null;
	refreshRate = 10;
	callid = null;
	connected = false;
	
    constructor() 
    {
        super();
    }

	connectedCallback()
	{
		super.connectedCallback();
		
		this.subscribeToCoordinator();
		
		this.startAutoRefresh();
	}
	
	disconnectedCallback()
	{
	    super.disconnectedCallback();
		
		if (this.callid != null)
			jadex.terminateCall(this.callid);
	}
	
	unsubscribeFromCoordinator()
	{
		if (this.callid != null)
			jadex.terminateCall(this.callid);
	}
	
	subscribeToCoordinator()
	{
		let self = this;
		
		CoordinatorApp.getInstance().clearRegistries();
		
		this.unsubscribeFromCoordinator();
		
		console.log("coordinator subscription starting");
		
		this.callid = jadex.getIntermediate("api/subscribe", res =>
		{
			self.setConnectionStatus(true);
			
            console.log("coordinator subscription result: ", res.data);
			
			// must save events due to start time?! save in other form?
			var event = res.data;
			if(event.type === 0)
            {
                CoordinatorApp.getInstance().addRegistry(event);
            }
            else if(event.type === 1)
            {
                CoordinatorApp.getInstance().removeRegistry(event);
            }
            else
            {
            	console.log("Unknown event type: " + event.type);
            }
		}, ex =>
		{
			self.setConnectionStatus(false);
			console.log("ui subscription error with agent, reconnect in 5 seconds: "+ex);
			setTimeout(() => self.subscribeToCoordinator(), 5000);
		}); 
    }
	
	setConnectionStatus(connected)
	{
		this.connected = connected;
		this.update();
	}

    appChanged(type, value) 
    {
        if (type === 'registries') 
        {
            this.update();
        }
    }
	
	loadDefaultStyle() 
	{
	    return this.loadStyle("./api/style.css");
	}
	
	computeUptime(start)
	{
	    if (!start) 
			return 'n/a';
		
        const millis = Date.now() - start;
        return this.formatUptime(millis);
	}
	
	formatUptime(millis) 
	{
		if (!millis) 
			return 'n/a';
		
		const totalSeconds = Math.floor(millis / 1000);
		const hours = Math.floor(totalSeconds / 3600);
		const minutes = Math.floor((totalSeconds % 3600) / 60);
		const seconds = totalSeconds % 60;

		const parts = [];
		if (hours > 0) parts.push(hours + 'h');
		if (minutes > 0 || hours > 0) parts.push(minutes + 'm');
		parts.push(seconds + 's');

		return parts.join(' ');
	}
	
	formatDate(millis) 
	{
		if (!millis) 
			return 'n/a';
		
		const date = new Date(millis);
		
		const pad = n => n.toString().padStart(2, '0');

		const day = pad(date.getDate());
		const month = pad(date.getMonth() + 1); // Monate 0-11
		const year = date.getFullYear();
		const hours = pad(date.getHours());
		const minutes = pad(date.getMinutes());
		const seconds = pad(date.getSeconds());

		return `${day}.${month}.${year} ${hours}:${minutes}:${seconds}`;
	}
	
	startAutoRefresh(rate) 
	{
		if(rate!=undefined)
			this.refreshRate = rate;
		
		console.log("Starting auto-refresh with rate: " + this.refreshRate + " seconds");
		
		let self = this;
		
	    if (this.refresher) 
			clearInterval(this.refresher);

	    if (this.refreshRate > 0) 
		{
	    	this.refresher = setInterval(() => 
	    	{
	       		console.log("Refreshing registry table");
				self.update();
	    	}, this.refreshRate * 1000);
	    }
	}
	
	getHTML() 
    {
    	//console.log("RegistryTableElement.getHTML() called");
        var regs = this.getApp().getRegistries();
		regs.sort((a, b) => (a?.startTime ?? 0) - (b?.startTime ?? 0));
		const oldestreg = regs[0];
		return `
			<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
		    <style>
				.refresh-control {
					max-width: 300px;
					width: 200px;
				}
				
				.connection-status {
					display: flex;
					align-items: center;
					gap: 0.5em;
					margin: 10px 0;
					font-family: sans-serif;
				}

				.status-indicator {
					width: 12px;
					height: 12px;
					border-radius: 50%;
				 	background-color: gray;
				}

				.status-indicator.online {
					background-color: #4caf50; /* grün */
				}

				.status-indicator.offline {
					background-color: #f44336; /* rot */
				}
				
				.refresh-control {
					display: flex;
					flex-direction: column;
					gap: 0.5em;
					max-width: 250px;
				}
				
				.refresh-label {
					font-family: sans-serif;
				}
				
				.status-overlay {
					position: fixed;
					top: 10px;
					right: 10px;
					z-index: 100;
					display: flex;
					flex-direction: column;
					align-items: flex-end;
				}

				.status-dot {
					width: 14px;
					height: 14px;
					border-radius: 50%;
					background-color: gray;
					transition: background-color 0.3s;
					cursor: pointer;
				}

				.status-overlay.online .status-dot {
					background-color: #4caf50;
				}
				.status-overlay.offline .status-dot {
					background-color: #f44336;
				}

				.status-panel {
					display: none;
					background: white;
					border: 1px solid #ccc;
					box-shadow: 0 2px 6px rgba(0,0,0,0.2);
					padding: 10px;
					border-radius: 6px;
					margin-top: 5px;
					width: 200px;
					font-family: sans-serif;
				}

				.status-overlay:hover .status-panel {
					display: block;
				}

				.status-text {
					font-weight: bold;
					cursor: pointer;
					color: #007bff;
				}

				.clickable:hover {
					text-decoration: underline;
				}        
				
				.icon-button {
					background: none;
					border: none;
					cursor: pointer;
					font-size: 1.2em;
					color: #444;
					padding: 4px;
				}
				
				.icon-button:hover {
					color: #007bff;
				}          
				
				.refresh-buttons {
				 	display: flex;
					gap: 4px;
					font-family: sans-serif;
				}
				
				.refresh-buttons button {
				 	padding: 3px 6px;
				 	font-size: 0.85em;
				 	border: 1px solid #ccc;
				 	background: #f9f9f9;
				 	cursor: pointer;
					border-radius: 4px;
				}
				
				.refresh-buttons button.active {
					background: #007bff;
					color: white;
					border-color: #007bff;
				}
				
				.refresh-compact {
					display: flex;
					align-items: center;
					gap: 0.5em;
				}
				
				.refresh-buttons button:hover {
					background-color: #eee;
				}
				
				.status-overlay {
					position: fixed;
					top: 10px;
					right: 10px;
					z-index: 100;
					font-family: sans-serif;
				}

				.status-header {
					display: flex;
					align-items: center;
					gap: 0.4em;
					cursor: pointer;
					padding-right: 20px;
					padding-top: 10px;
				}

				.status-dot {
					width: 12px;
					height: 12px;
					border-radius: 50%;
					background-color: gray;
					flex-shrink: 0;
				}

				.status-overlay.online .status-dot {
					background-color: #4caf50;
				}

				.status-overlay.offline .status-dot {
					background-color: #f44336;
				}

				.refresh-status {
					display: inline-flex;
					align-items: center;
					gap: 0.25em;
					font-size: 0.85em;
					white-space: nowrap;
				}

				.status-panel {
					display: none;
					background: white;
					border: 1px solid #ccc;
					box-shadow: 0 2px 6px rgba(0,0,0,0.2);
					padding: 10px;
					border-radius: 6px;
					margin-top: 5px;
				}
				
				.status-overlay:hover .status-panel {
					display: block;
				}

				.oldest-label {
					font-size: 0.7em;
					color: #666;
					margin-left: 4px;
				}
				.highlight-oldest {
					/*background-color: #fff59d; */
					/*font-weight: bold;*/
				}
			</style>
			<div>
				<div class="status-overlay ${this.connected ? 'online' : 'offline'}">
					<div class="status-header">
				    	<span class="status-dot"></span>
				    	<span class="refresh-status">
				      		${this.refreshRate > 0 ? `${this.refreshRate}s <i class="fas fa-arrows-rotate"></i>` : 'off'}
				    	</span>
				  	</div>
					<div class="status-panel">
						<div class="refresh-compact">
							<i class="fas fa-arrows-rotate" title="Auto-refresh"></i>
							<span class="refresh-buttons">
	  							<button data-val="0" onclick="this.getRootNode().host.startAutoRefresh(0); this.getRootNode().host.update()">off</button>
	  							<button data-val="5" onclick="this.getRootNode().host.startAutoRefresh(5); this.getRootNode().host.update()">5s</button>
	  							<button data-val="10" onclick="this.getRootNode().host.startAutoRefresh(10); this.getRootNode().host.update()">10s</button>
	  							<button data-val="30" onclick="this.getRootNode().host.startAutoRefresh(30); this.getRootNode().host.update()">30s</button>
	  						</span>
							<button class="icon-button" title="Reconnect" onclick="this.getRootNode().host.subscribeToCoordinator()">
								<i class="fas fa-plug-circle-bolt"></i>
							</button>
						</div>
					</div>
				</div>
			    <table class="styled-table">
			        <thead>
			            <tr>
			                <th>Name</th>
			                <th>Start time</th>
							<th>Uptime</th>
			                <th>Groups</th>
			                <th>Unrestricted</th>
			            </tr>
			        </thead>
			        <tbody>
			            ${regs.map(c => {
							const isoldest = c === oldestreg;
							return `
							<tr class="${isoldest ? 'highlight-oldest' : ''}">
								<td>${c?.service?.providerId} ${isoldest ? '&#127775; <span class="oldest-label">(longest running)</span>' : ''}</td>
								<td>${this.formatDate(c?.startTime)}</td>
								<td>${this.computeUptime(c?.startTime)}</td>
								<td>${c?.service?.groupNames?.map(g => `${g}`).join(' ') || ''}</td>
								<td>${c?.service?.unrestricted ? 'Yes' : 'No'}</td>
							</tr>`;
						}).join('')}
			        </tbody>
			    </table>
			</div>
		`;
    }
}

if(customElements.get('registry-table') === undefined)
	customElements.define('registry-table', RegistryTableElement);
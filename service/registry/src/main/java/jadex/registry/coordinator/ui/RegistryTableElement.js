import BaseElement from './coordinatorapi/BaseElement.js';

export class RegistryTableElement extends BaseElement 
{
    constructor() 
    {
        super();
    }

    getHTML() 
    {
    	console.log("RegistryTableElement.getHTML() called");
        var regs = this.getApp().getRegistries();
        return `
            <table>
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Start time</th>
                    	<th>Groups</th>
						<th>Unrestricted</th>
                    </tr>
                </thead>
                <tbody>
                    ${regs.map(c => `
                        <tr>
                            <td>${c?.service?.providerId}</td>
                            <td>${c?.startTime}</td>
							<td>${c?.service?.groupNames?.map(g => `${g}`).join(' ') || ''}</td>
							<td>${c?.service?.unrestricted ? 'Yes' : 'No'}</td>
                        </tr>`).join('')}
                </tbody>
            </table>`;
    }

    appChanged(type, value) 
    {
        if (type === 'registries') 
        {
            this.update();
        }
    }
}

if(customElements.get('registry-table') === undefined)
	customElements.define('registry-table', RegistryTableElement);
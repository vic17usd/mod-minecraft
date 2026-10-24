// Dirección artística por material: máscaras de incrustación distintas, no una plantilla recoloreada.
export function generateNavigation({ png,json,read,id,bodies,assets,data }) {
    const hash = (x,y,s) => { let n = Math.imul(x,374761393) ^ Math.imul(y,668265263) ^ Math.imul(s,1274126177); n = Math.imul(n ^ n >>> 13,1274126177); return ((n ^ n >>> 16) >>> 0) / 4294967295; };
    const clamp = color => color.map(c => Math.max(0,Math.min(255,Math.round(c))));
    const blend = (a,b,t) => a.map((c,i)=>c*(1-t)+b[i]*t);
    const ridge = (d,width) => Math.max(0,1-Math.abs(d)/width);
    const periodic = (v,period) => { const a = ((v % period)+period)%period; return Math.min(a,period-a); };
    const palettes = [[137,139,146],[83,80,76],[55,47,43],[157,74,48],[98,91,91],[121,115,110],
        [148,103,70],[185,147,43],[157,182,196],[132,142,152],[101,108,116],[178,150,101],
        [150,105,53],[186,212,224],[142,155,175],[162,187,198],[117,144,165],[162,175,187],
        [88,79,79],[93,158,171],[127,170,181],[93,108,126],[120,142,170],[113,124,147],
        [120,144,164],[59,82,151],[144,170,183],[92,109,130]];
    const patterns = ['parallel_filaments','metal_needles','sulfur_vesicles','angular_ladder','triangular_shards','isolated_lenses',
        'cyclone_ellipses','volcanic_fissures','hexagonal_pockets','braided_ribbons','impact_rings','oblique_strata',
        'amber_dendrites','tiger_stripes','radial_fan','diamond_nodules','binary_clusters','sawtooth_seams',
        'bipolar_facets','tabular_grid','curved_channels','dark_druzy','polygon_web','branching_tree',
        'stepped_maze','storm_spiral','snowflake_stars','polygon_geodes'];
    const manifest = [];
    for (const [index,body] of bodies.filter(body=>body.id!=='earth').entries()) {
        const seed=4387+index*1999,base=palettes[index],gem=[80+hash(1,0,seed)*170,110+hash(2,0,seed)*135,100+hash(3,0,seed)*145];
        if(body.id==='venus'||body.id==='io') gem.splice(0,3,240,184,39);
        if(body.id==='mars') gem.splice(0,3,105,171,139);
        const centers=Array.from({length:8},(_,i)=>[hash(i,3,seed)*128,hash(i,5,seed)*128,4+hash(i,8,seed)*9]);
        const nearest=(x,y,mode)=>Math.max(...centers.map(([cx,cy,r])=>mode(x-cx,y-cy,r)));
        function inclusion(x,y) {
            const r=Math.hypot(x-64,y-64),a=Math.atan2(y-64,x-64);
            switch(index) {
                case 0:return ridge(periodic(y+Math.sin(x/17)*8,27),1.8);
                case 1:return nearest(x,y,(dx,dy,s)=>Math.abs(dy+dx*.65)<1.8&&Math.abs(dx)<s*2?1:0);
                case 2:return nearest(x,y,(dx,dy,s)=>ridge(Math.hypot(dx,dy)-s,2.6));
                case 3:return Math.max(ridge(periodic(x+y*.42,39),2.2),periodic(y,25)<2&&periodic(x+y*.42,39)<12?.8:0);
                case 4:return nearest(x,y,(dx,dy,s)=>Math.abs(dx)/s+Math.abs(dy-dx*.6)/s<1?1:0);
                case 5:return nearest(x,y,(dx,dy,s)=>dx*dx/(s*s*3)+dy*dy/(s*s*.3)<1?1:0);
                case 6:return ridge(periodic(Math.hypot((x-64)*.7,y-64)+Math.sin(a*3)*5,23),3);
                case 7:return ridge(periodic(x-y*.36+Math.sin(y/7)*5,41),2.6);
                case 8:return nearest(x,y,(dx,dy,s)=>Math.max(Math.abs(dx)*.866+Math.abs(dy)*.5,Math.abs(dy))<s?1:0);
                case 9:return Math.max(ridge(periodic(y-Math.sin(x/16)*13,53),3),ridge(periodic(y+Math.sin(x/16)*13+8,53),2));
                case 10:return nearest(x,y,(dx,dy,s)=>ridge(Math.hypot(dx,dy)-s,1.4)+ridge(Math.hypot(dx,dy)-s*.48,1.2)*.7);
                case 11:return ridge(periodic(y+x*.72,31),4)*(periodic(x,39)>5?1:.2);
                case 12:return Math.max(ridge(periodic(y-Math.sin(x/27)*17,57),2),periodic(x+y,27)<2&&periodic(y-Math.sin(x/27)*17,57)<15?.8:0);
                case 13:return ridge(periodic(x+Math.sin(y/28)*17,37),2.5)*(Math.sin(y/8)>-.4?1:.5);
                case 14:return r>16&&r<62?ridge(periodic(a*20,Math.PI*2),.38):0;
                case 15:return nearest(x,y,(dx,dy,s)=>Math.abs(dx)/s+Math.abs(dy)/(s*1.6)<1?1:0);
                case 16:return nearest(x,y,(dx,dy,s)=>Math.max(ridge(Math.hypot(dx-4,dy)-s*.35,3),ridge(Math.hypot(dx+5,dy+4)-s*.4,2)));
                case 17:return ridge(periodic(y+(periodic(x,32)-8)*1.5,37),2.5);
                case 18:return (x<64?nearest(x,y,(dx,dy,s)=>Math.max(0,1-(Math.abs(dx)+Math.abs(dy))/s)) :ridge(periodic(x-y*.7,21),3))*.9;
                case 19:return periodic(x,35)<8&&periodic(y,29)<7?1:0;
                case 20:return ridge(periodic(y-Math.sin(x/40)*30,43),4)*(periodic(x,47)>4?1:0);
                case 21:return nearest(x,y,(dx,dy,s)=>Math.max(0,1-Math.max(Math.abs(dx),Math.abs(dy))/s))*(hash(x,y,seed)>.35?1:.15);
                case 22:{const distances=centers.map(([cx,cy])=>Math.hypot(x-cx,y-cy)).sort((a,b)=>a-b);return ridge(distances[1]-distances[0],3);}
                case 23:return Math.max(ridge(x-64,2.5),...[-1,1].flatMap(side=>[24,48,72,96].map(k=>ridge(y-k-side*(x-64)*.8,2)*(Math.abs(x-64)<18?1:0))));
                case 24:return (periodic(x,32)<3&&periodic(y+Math.floor(x/32)*9,42)<30)||(periodic(y,42)<3&&periodic(x,32)<24)?1:0;
                case 25:return r>9?ridge(periodic(r-a*11,31),3.5):0;
                case 26:return nearest(x,y,(dx,dy,s)=>Math.hypot(dx,dy)<s*1.4?ridge(periodic(Math.atan2(dy,dx)*6,Math.PI*2),.32):0);
                default:return nearest(x,y,(dx,dy,s)=>ridge(Math.max(Math.abs(dx),Math.abs(dy)*.85)-s,2.8));
            }
        }
        function rock(x,y,soil=false) {
            const grain=(hash(x,y,seed)-.5)*(soil?27:13),bedding=Math.sin((x*.23+y*(.18+index*.01)))*7;
            let color=base.map(c=>c+grain+bedding);
            if(index===2||index===7) { const pore=nearest(x,y,(dx,dy,s)=>ridge(Math.hypot(dx,dy)-s*.6,2));color=color.map(c=>c-pore*29); }
            else if(index===3) {const crack=periodic(x+y*.62+Math.floor(y/29)*11,47);if(crack<1.6)color=color.map(c=>c*.39);if(crack<6&&crack>=1.6)color=blend(color,[166,71,32],.45);}
            else if(index===0&&soil) { const crater=nearest(x,y,(dx,dy,s)=>Math.max(0,1-Math.hypot(dx,dy)/s));color=color.map(c=>c-crater*31); }
            else {
                const cuts=periodic((index%2?x:y)+(index%3===0?Math.floor(x/21)*9:Math.sin(y/(13+index))*8),28+index);
                if(cuts<(soil?.5:1.5))color=color.map(c=>c*(soil?.9:.57));
                if(index>7&&index%3===0&&periodic(x-y*.4,31)<2)color=color.map(c=>c+24);
            }
            return clamp(color);
        }
        for(const suffix of ['stone','regolith','ore','bricks']) png(`block/${body.id}_${suffix}`,128,128,(x,y)=>{
            if(suffix==='bricks') {
                const rowHeight=24+index%3*8,offset=Math.floor(y/rowHeight)%2?23:0;
                const seam=periodic(y,rowHeight)<2||periodic(x+offset,48+index%3*8)<2;
                return seam?clamp(base.map(c=>c*.38)):clamp(rock(x,y).map(c=>c+ (y%rowHeight<5?13:0)));
            }
            const host=rock(x,y,suffix==='regolith');if(suffix!=='ore')return host;
            const mask=Math.min(1,inclusion(x,y));if(mask<=0)return host;
            const relief=(inclusion(x-1,y-1)-inclusion(x+1,y+1))*27;
            return clamp(blend(host,gem.map(c=>c*(.75+hash(Math.floor(x/4),Math.floor(y/4),seed)*.35)+relief),mask));
        });
        // El recurso extraído comparte la paleta y las inclusiones del bloque, con silueta propia.
        png(`item/${body.resource}`,64,64,(x,y)=>{
            const dx=x-32,dy=y-32,a=Math.atan2(dy,dx),r=Math.hypot(dx,dy);
            let inside=r<20+Math.sin(a*(3+index%5)+index)*4+Math.sin(a*9+seed)*2;
            if(index===0)inside=Math.abs(dx+Math.sin(y/8)*4)<8&&Math.abs(dy)<26;
            if(index===1)inside=Math.abs(dx+dy*.6)<4&&Math.abs(dy)<27||Math.abs(dx-10+dy*.6)<3&&Math.abs(dy)<19;
            if(index===3)inside=Math.abs(dx)<24&&Math.abs(dy+dx*.22)<11;
            if(!inside)return[0,0,0,0];
            const facet=.67+(1-r/35)*.35+((x+y+index)%17<2?.13:0);
            const color=blend(gem,base,Math.min(.4,inclusion(x*2,y*2)*.4));
            return clamp(color.map(c=>c*facet+8*Math.sin(a*4-index)+(hash(x,y,seed)-.5)*8));
        });
        const name=`${body.id}_bricks`;
        json(`${assets}/blockstates/${name}.json`,{variants:{'':{model:id(`block/${name}`)}}});
        json(`${assets}/models/block/${name}.json`,{parent:'minecraft:block/cube_all',textures:{all:id(`block/${name}`)}});
        json(`${assets}/models/item/${name}.json`,{parent:id(`block/${name}`)});
        json(`${data}/loot_table/blocks/${name}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:id(name)}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
        json(`${data}/recipe/${name}.json`,{type:'minecraft:crafting_shaped',category:'building',pattern:['SS','SS'],key:{S:{item:id(`${body.id}_stone`)}},result:{id:id(name),count:4}});
        manifest.push({body:body.id,ore_pattern:patterns[index],texture_size:128});
    }
    json('texture_styles.json',manifest);
    png('block/titanium_ore',128,128,(x,y)=>{
        const chip=periodic(x+Math.floor(y/19)*11,37)<11&&periodic(y,19)<6;
        return clamp((chip?[139,193,211]:[76,88,108]).map(c=>c+(hash(x,y,19)-.5)*17+(chip&&y%19<2?28:0)));
    });
    for(const name of ['reinforced_panel','vent_grate','energy_cell']) {
        png(`block/${name}`,128,128,(x,y)=>{
            const noise=(hash(x,y,72)-.5)*10;
            if(name==='reinforced_panel') {
                const diagonal=Math.abs(x-y)<4||Math.abs(x+y-127)<4,border=x<7||y<7||x>120||y>120;
                const bolt=[12,115].some(cx=>[12,115].some(cy=>Math.hypot(x-cx,y-cy)<4));
                return clamp((bolt?[45,57,68]:border?[86,104,121]:diagonal?[124,146,163]:[46,65,85]).map(c=>c+noise));
            }
            if(name==='vent_grate') {
                const frame=x<9||x>118||y<9||y>118,mesh=x%12<4||y%12<4;
                return clamp((frame?[127,144,151]:mesh?[69,88,99]:[12,22,31]).map(c=>c+noise+(y%12===3?13:0)));
            }
            const frame=x<10||x>117||y<10||y>117,bar=periodic(x,24)<4||periodic(y,32)<3;
            return clamp((frame?[43,54,73]:bar?[15,52,68]:[34,169,190]).map(c=>c+noise+(frame?0:Math.sin(y/9)*20)));
        });
        json(`${assets}/blockstates/${name}.json`,{variants:{'':{model:id(`block/${name}`)}}});
        json(`${assets}/models/block/${name}.json`,{parent:'minecraft:block/cube_all',textures:{all:id(`block/${name}`)}});
        json(`${assets}/models/item/${name}.json`,{parent:id(`block/${name}`)});
        json(`${data}/loot_table/blocks/${name}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:id(name)}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
        json(`${data}/recipe/${name}.json`,{type:'minecraft:crafting_shaped',category:'building',pattern:['TTT','TCT','TTT'],key:{T:{item:id('titanium_ore')},C:{item:name==='energy_cell'?'minecraft:redstone':name==='vent_grate'?'minecraft:iron_bars':'minecraft:iron_ingot'}},result:{id:id(name),count:4}});
    }
    // Iconografía esférica por cuerpo; el mapa es esquemático, no una simulación de distancias orbitales.
    for(const [index,body] of [{id:'sun'},...bodies].entries()) png(`gui/celestial/${body.id}`,32,32,(x,y)=>{
        const dx=x-15.5,dy=y-15.5,ring=body.id==='saturn'||body.id==='uranus';
        const radius=ring?9:13,r=Math.hypot(dx,dy);
        if(r>radius) {
            if(ring&&Math.abs(dy+dx*.27)<1.8&&Math.abs(dx)<15)return [172,158,123,220];
            return [0,0,0,0];
        }
        const color=body.id==='sun'?[255,186,53]:body.id==='earth'?[49,134,186]:palettes[Math.max(0,bodies.filter(b=>b.id!=='earth').findIndex(b=>b.id===body.id))];
        const nz=Math.sqrt(Math.max(0,1-r*r/(radius*radius))),shade=.35+Math.max(0,(-dx*.5-dy*.4)/radius+nz*.6)*.65;
        const bands=['jupiter','saturn','neptune'].includes(body.id)?Math.sin(y*1.5)*18:hash(x,y,index)*13;
        return clamp(color.map(c=>c*shade+bands));
    });
    const additional=[...bodies.filter(b=>b.id!=='earth').map(b=>id(`${b.id}_bricks`)),...['reinforced_panel','vent_grate','energy_cell'].map(id)];
    for(const tag of ['mineable/pickaxe','needs_iron_tool']) {const file=`data/minecraft/tags/block/${tag}.json`,value=read(file);value.values=[...new Set([...value.values,...additional])];json(file,value);}
    const phrases={
        'system.galaxycraft.sol':['Sistema Sol','Sol System'], 'key.galaxycraft.navigation':['Abrir mapa celestial','Open celestial map'],
        'navigation.galaxycraft.title':['GALAXYCRAFT · ORDENADOR DE NAVEGACIÓN','GALAXYCRAFT · NAVIGATION COMPUTER'],
        'navigation.galaxycraft.navigate':['Navegar','Navigate'], 'navigation.galaxycraft.back':['Atrás','Back'],
        'navigation.galaxycraft.controls':['Combustible','Fuel controls'], 'navigation.galaxycraft.land':['Confirmar despegue','Confirm launch'],
        'navigation.galaxycraft.galaxy':['Sistemas estelares','Star systems'], 'navigation.galaxycraft.planets':['Planetas','Planets'], 'navigation.galaxycraft.moons':['Superficie y lunas','Surface and moons'],
        'navigation.galaxycraft.rocket_tier':['Cohete N%s','Rocket T%s'], 'navigation.galaxycraft.required':['Requiere N%s','Requires T%s'],
        'navigation.galaxycraft.cost':['Coste: %s mB','Cost: %s mB'], 'navigation.galaxycraft.reserve':['Reserva: %s mB','Reserve: %s mB'],
        'navigation.galaxycraft.gravity':['Gravedad: %s g','Gravity: %s g'],
        'navigation.galaxycraft.system_tier':['Sistema disponible desde cohetes N%s.','System available to tier %s rockets.'],
        'navigation.galaxycraft.body_tooltip':['%s · Nivel %s · %s mB de combustible','%s · Tier %s · %s mB fuel'],
        'navigation.galaxycraft.no_computer':['Acércate a un cohete o abre sus controles para navegar.','Approach a rocket or open its flight controls to navigate.'],
        'navigation.galaxycraft.reason.ready':['Destino confirmado. Listo para encender.','Destination confirmed. Ready to ignite.'],
        'navigation.galaxycraft.reason.choose':['Selecciona sistema, planeta y superficie o luna.','Choose a system, planet, and surface or moon.'],
        'navigation.galaxycraft.reason.sync':['Esperando confirmación del servidor…','Waiting for server confirmation…'],
        'navigation.galaxycraft.reason.tier':['El nivel del cohete es insuficiente.','Rocket tier is too low.'],
        'navigation.galaxycraft.reason.fuel':['Necesitas 4000 mB de lava para el trayecto.','This trip requires 4000 mB of lava.'],
        'navigation.galaxycraft.reason.pad':['Completa la plataforma 3 × 3 bajo el cohete.','Complete the 3 × 3 launch pad below the rocket.'],
        'navigation.galaxycraft.reason.clearance':['Despeja el volumen de 3 × 3 × 7 del cohete.','Clear the rocket’s 3 × 3 × 7 volume.'],
        'navigation.galaxycraft.reason.same_world':['Ya estás en este destino.','You are already at this destination.'],
        'navigation.galaxycraft.reason.oxygen':['Equipa casco y lleva un tanque con oxígeno.','Equip a helmet and carry a charged oxygen tank.'],
        'navigation.galaxycraft.reason.unavailable':['El destino o sistema no está disponible.','Destination or system unavailable.'],
        'navigation.galaxycraft.reason.igniting':['Hay un encendido en curso.','An ignition sequence is already running.'],
        'navigation.galaxycraft.reason.pilot':['El piloto no puede iniciar un viaje.','The pilot cannot start a journey.'],
        'block.galaxycraft.reinforced_panel':['Panel reforzado','Reinforced Panel'], 'block.galaxycraft.vent_grate':['Rejilla de ventilación','Vent Grate'],
        'block.galaxycraft.energy_cell':['Celda energética luminosa','Luminous Energy Cell']
    };
    for(const [i,language] of ['es_es','en_us'].entries()) {
        const file=`${assets}/lang/${language}.json`,texts=read(file);
        for(const [key,values] of Object.entries(phrases))texts[key]=values[i];
        for(const body of bodies.filter(b=>b.id!=='earth')) {
            texts[`block.galaxycraft.${body.id}_bricks`]=i===0?`Ladrillos de ${body.name}`:`${body.id[0].toUpperCase()+body.id.slice(1)} Bricks`;
            texts[`tooltip.galaxycraft.detail.${body.id}_bricks`]=i===0?`Mampostería fabricada con roca de ${body.name}.`:`Masonry crafted from ${body.id} stone.`;
        }
        for(const name of ['reinforced_panel','vent_grate','energy_cell'])texts[`tooltip.galaxycraft.detail.${name}`]=i===0
            ?name==='energy_cell'?'Bloque decorativo luminoso; no almacena energía ni sustituye el combustible del cohete.':'Construcción industrial de titanio; necesita pico de hierro.'
            :name==='energy_cell'?'Luminous decorative block; does not store energy or replace rocket fuel.':'Titanium industrial construction; requires an iron pickaxe.';
        json(file,texts);
    }
    console.log('Navegación: iconos celestiales, 28 patrones minerales distintos, ladrillos y construcción industrial.');
}
